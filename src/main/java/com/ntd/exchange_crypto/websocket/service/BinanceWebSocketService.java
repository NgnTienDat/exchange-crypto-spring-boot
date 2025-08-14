package com.ntd.exchange_crypto.websocket.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntd.exchange_crypto.market.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import lombok.Setter;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BinanceWebSocketService {

    private final ApplicationEventPublisher eventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private WebSocketSession tickerSession; // Session chung cho ticker
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final List<String> tickerSymbols;
    private final Set<String> activeDepthSubscriptions = new HashSet<>(); // Theo dõi depth
    private final Map<String, WebSocketSession> productSessions = new ConcurrentHashMap<>();

    @NonFinal
    @Value("${app.binance.websocket-url}")
    protected String BINANCE_WEBSOCKET_URL;

    public BinanceWebSocketService(ApplicationEventPublisher eventPublisher, RedisTemplate<String, Object> redisTemplate,
                                   ObjectMapper objectMapper,
                                   @Value("#{'${app.binance.ticker-symbols}'.split(',')}") List<String> tickerSymbols) {
        this.eventPublisher = eventPublisher;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.tickerSymbols = tickerSymbols;
    }

    @PostConstruct
    @Async("binanceExecutor")
    public void connect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.setDefaultMaxTextMessageBufferSize(15 * 1024 * 1024); // 5MB
            container.setDefaultMaxBinaryMessageBufferSize(15 * 1024 * 1024);

            WebSocketClient webSocketClient = new StandardWebSocketClient(container);
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            log.info("URL WS {}", BINANCE_WEBSOCKET_URL);
            tickerSession = webSocketClient.execute(
                    new BinanceWebSocketHandler(null, true), // Handler chung cho ticker
                    headers,
                    URI.create(BINANCE_WEBSOCKET_URL)
            ).get();

            scheduler.scheduleAtFixedRate(this::sendHeartbeat, 30, 30, TimeUnit.SECONDS);
            subscribeToTicker();

        } catch (Exception e) {
            log.error("Failed to connect to Binance WebSocket for ticker", e);
            scheduleReconnect();
        }
    }

    private void sendHeartbeat() {
        try {
            if (tickerSession != null && tickerSession.isOpen()) {
                tickerSession.sendMessage(new PingMessage());
                log.debug("Heartbeat sent to Binance WebSocket for ticker");
            } else {
                log.warn("Cannot send heartbeat - Ticker WebSocket session is not open");
                scheduleReconnect();
            }
        } catch (Exception e) {
            log.error("Failed to send heartbeat for ticker", e);
            scheduleReconnect();
        }
    }

    private void subscribeToTicker() {
        try {
            if (tickerSession != null && tickerSession.isOpen()) {
                List<String> tickerStreams = tickerSymbols.stream()
                        .map(symbol -> "\"" + symbol.trim().toLowerCase() + "@ticker\"")
                        .collect(Collectors.toList());

                String subscriptionMessage = String.format(
                        "{\"method\": \"SUBSCRIBE\", \"params\": [%s], \"id\": 1}",
                        String.join(", ", tickerStreams)
                );

                tickerSession.sendMessage(new TextMessage(subscriptionMessage));
                log.info("Subscribed to ticker streams: {}", tickerSymbols);
            } else {
                log.warn("Cannot subscribe to ticker - WebSocket session is not open");
            }
        } catch (Exception e) {
            log.error("Failed to subscribe to ticker streams", e);
        }
    }


    public synchronized void subscribeToDepth(String productId) {
        try {
            if (!activeDepthSubscriptions.contains(productId)) {
                String depthStream = productId.toLowerCase() + "@depth20";
                String tradeStream = productId.toLowerCase() + "@trade";
                String wsUrl = BINANCE_WEBSOCKET_URL;

                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                container.setDefaultMaxTextMessageBufferSize(15 * 1024 * 1024);
                container.setDefaultMaxBinaryMessageBufferSize(15 * 1024 * 1024);

                WebSocketClient webSocketClient = new StandardWebSocketClient(container);
                WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
                WebSocketSession session = webSocketClient.execute(
                        new BinanceWebSocketHandler(productId, false), // Handler cho cả depth và kline
                        headers,
                        URI.create(wsUrl)
                ).get();

                productSessions.put(productId, session);
                activeDepthSubscriptions.add(productId);

                // Subscribe to both depth and kline streams
                String subscriptionMessage = "{\"method\": \"SUBSCRIBE\", " +
                        "\"params\": [\"" + depthStream + "\", \"" + tradeStream + "\"], " +
                        "\"id\": 2}";

                session.sendMessage(new TextMessage(subscriptionMessage)); //send subscribe

                log.info("Subscribed to depth20 and trade streams for product: {} at {}", productId, wsUrl);
            } else {
                log.debug("Already subscribed to depth and trade streams for product: {}", productId);
            }
        } catch (Exception e) {
            log.error("Failed to subscribe to depth and trade streams for product: {}", productId, e);
            reconnect(productId);
        }
    }


    public synchronized void unsubscribeFromDepth(String productId) {
        try {
            WebSocketSession session = productSessions.get(productId);
            if (session != null && session.isOpen()) {
                String depthStream = productId.toLowerCase() + "@depth20";
                String tradeStream = productId.toLowerCase() + "@trade";

                session.sendMessage(new TextMessage(
                        "{\"method\": \"UNSUBSCRIBE\", " +
                                "\"params\": [\"" + depthStream + "\", \"" + tradeStream + "\"], " +
                                "\"id\": 3}"
                ));
                session.close();
                productSessions.remove(productId);
                activeDepthSubscriptions.remove(productId);
                log.info("Unsubscribed from depth and trade stream of product: {}", productId);
            } else {
                log.debug("No active session to unsubscribe for product: {}", productId);
            }
        } catch (Exception e) {
            log.error("Failed to unsubscribe from depth and trade stream for product: {}", productId, e);
        }
    }

    private void handleTickerMessage(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);

            if ("24hrTicker".equals(rootNode.get("e").asText())) {
                String productId = rootNode.get("s").asText();
                BigDecimal price = new BigDecimal(rootNode.get("c").asText());
                BigDecimal priceChangePercent24h = new BigDecimal(rootNode.get("P").asText());
                BigDecimal volume24h = new BigDecimal(rootNode.get("v").asText());
                BigDecimal low24h = new BigDecimal(rootNode.get("l").asText());
                BigDecimal high24h = new BigDecimal(rootNode.get("h").asText());

                MarketData marketData = MarketData.builder()
                        .productId(productId)
                        .price(price)
                        .priceChangePercent24h(priceChangePercent24h)
                        .volume24h(volume24h)
                        .low24h(low24h)
                        .high24h(high24h)
                        .timestamp(Instant.ofEpochMilli(rootNode.get("E").asLong()))
                        .build();

                eventPublisher.publishEvent(new MarketDataReceivedEvent(marketData));
            }
        } catch (Exception e) {
            log.error("Error processing ticker message: {}", message, e);
        }
    }


    private void handleDepthMessage(String message, String productId) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);

            // Xử lý dữ liệu @depth20
            if (rootNode.has("lastUpdateId") && rootNode.has("bids") && rootNode.has("asks")) {
                Long lastUpdateId = rootNode.get("lastUpdateId").asLong();
                JsonNode bidsNode = rootNode.get("bids");
                JsonNode asksNode = rootNode.get("asks");

                // Xử lý bids
                List<OrderBookEntry> bids = new ArrayList<>();
                if (bidsNode != null && bidsNode.isArray()) {
                    Iterator<JsonNode> bidsIterator = bidsNode.iterator();
                    int count = 0;
                    while (bidsIterator.hasNext() && count < 20) {
                        JsonNode bid = bidsIterator.next();
                        String priceLevel = bid.get(0).asText();
                        BigDecimal quantity = new BigDecimal(bid.get(1).asText());
                        bids.add(OrderBookEntry.builder()
                                .priceLevel(new BigDecimal(priceLevel))
                                .quantity(quantity)
                                .build());
                        count++;
                    }
                }

                // Xử lý asks
                List<OrderBookEntry> asks = new ArrayList<>();
                if (asksNode != null && asksNode.isArray()) {
                    Iterator<JsonNode> asksIterator = asksNode.iterator();
                    int count = 0;
                    while (asksIterator.hasNext() && count < 20) {
                        JsonNode ask = asksIterator.next();
                        String priceLevel = ask.get(0).asText();
                        BigDecimal quantity = new BigDecimal(ask.get(1).asText());
                        asks.add(OrderBookEntry.builder()
                                .priceLevel(new BigDecimal(priceLevel))
                                .quantity(quantity)
                                .build());
                        count++;
                    }
                }

                // Tạo và gửi sự kiện OrderBookData
                OrderBookData orderBookData = OrderBookData.builder()
                        .productId(productId)
                        .bids(bids)
                        .asks(asks)
                        .lastUpdateId(lastUpdateId)
                        .build();

                redisTemplate.convertAndSend("orderbook-to-stat:" + productId, orderBookData);
                eventPublisher.publishEvent(new OrderBookReceivedEvent(orderBookData));
            }

            // Xử lý dữ liệu @kline_1m
            if (rootNode.has("e") &&
                    rootNode.get("e").asText().equals("kline") &&
                    rootNode.has("k")) {

                JsonNode klineData = rootNode.get("k");
                long timestamp = klineData.get("t").asLong(); // Thời gian bắt đầu nến
                double open = klineData.get("o").asDouble(); // Giá mở cửa
                double high = klineData.get("h").asDouble(); // Giá cao nhất
                double low = klineData.get("l").asDouble();  // Giá thấp nhất
                double close = klineData.get("c").asDouble(); // Giá đóng cửa
                double volume = klineData.get("v").asDouble(); // Khối lượng giao dịch
                double totalVolume = klineData.get("q").asDouble(); // Khối lượng giao dịch
                boolean isFinal = klineData.get("x").asBoolean(); // Nến đã đóng hay chưa

                CandleStick candleStick = CandleStick.builder()
                        .productId(productId)
                        .timestamp(timestamp)
                        .open(open)
                        .high(high)
                        .low(low)
                        .close(close)
                        .volume(volume)
                        .totalVolume(totalVolume)
                        .isFinal(isFinal)
                        .build();

                eventPublisher.publishEvent(new CandleStickReceivedEvent(candleStick));
            }

            if (rootNode.has("e") &&
                    rootNode.get("e").asText().equals("trade")) {

                String tradeProductId = rootNode.get("s").asText(); // Sử dụng "s" từ dữ liệu trade
                Long tradeId = rootNode.get("t").asLong();         // Số thứ tự giao dịch
                BigDecimal price = new BigDecimal(rootNode.get("p").asText()); // Giá giao dịch
                BigDecimal quantity = new BigDecimal(rootNode.get("q").asText()); // Khối lượng
                Long tradeTime = rootNode.get("T").asLong();        // Thời gian giao dịch
                boolean isMaker = rootNode.get("m").asBoolean();    // True nếu maker, false nếu taker
                BigDecimal totalValue = price.multiply(quantity);   // Tính tổng giá trị

                MarketTrade marketTrade = MarketTrade.builder()
                        .productId(tradeProductId)
                        .tradeId(tradeId)
                        .price(price)
                        .quantity(quantity)
                        .tradeTime(tradeTime)
                        .isMaker(isMaker)
//                        .isBuyer(isBuyer)
                        .totalValue(totalValue)
                        .build();

                eventPublisher.publishEvent(new MarketTradeReceivedEvent(marketTrade));
            }
        } catch (Exception e) {
            log.error("Error processing depth or kline message for product {}: {}", productId, message, e);
        }
    }

    private void scheduleReconnect() {
        if (!scheduler.isShutdown()) {
            scheduler.schedule(() -> {
                log.info("Attempting to reconnect to Binance WebSocket for ticker");
                tickerSession = null; // Xóa session cũ
                connect(); // Tái kết nối ticker
            }, 5, TimeUnit.SECONDS);
        }
    }

    private void reconnect(String productId) {
        if (!scheduler.isShutdown()) {
            scheduler.schedule(() -> {
                log.info("Attempting to reconnect to Binance WebSocket for product: {}", productId);
                productSessions.remove(productId); // Xóa session cũ
                subscribeToDepth(productId); // Tái kết nối
            }, 5, TimeUnit.SECONDS);
        }
    }

    @PreDestroy
    public void disconnect() {
        if (tickerSession != null && tickerSession.isOpen()) {
            try {
                tickerSession.close();
            } catch (Exception e) {
                log.error("Error closing WebSocket session for ticker", e);
            }
        }
        productSessions.forEach((product, session) -> {
            try {
                if (session != null && session.isOpen()) {
                    session.close();
                }
            } catch (Exception e) {
                log.error("Error closing WebSocket session for product: {}", product, e);
            }
        });
        scheduler.shutdown();
    }

    private class BinanceWebSocketHandler extends TextWebSocketHandler {
        private final String identifier; // productId cho depth, null cho ticker
        private final boolean isTicker;
        @Setter
        private boolean disconnecting = false; // Flag to track intentional closures

        public BinanceWebSocketHandler(String identifier, boolean isTicker) {
            this.identifier = identifier;
            this.isTicker = isTicker;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            if (isTicker) {
                log.info("Connected to Binance WebSocket for ticker");
                BinanceWebSocketService.this.tickerSession = session; // Cập nhật session ticker
                subscribeToTicker(); // Subscribe ngay sau khi kết nối
            } else {
                log.info("Connected to Binance WebSocket for product: {}", identifier);
                productSessions.put(identifier, session);
                String depthStream = identifier.toLowerCase() + "@depth20";
//                String klineStream = identifier.toLowerCase() + "@kline_1m";
                String tradeStream = identifier.toLowerCase() + "@trade";


                String subscriptionMessage = "{\"method\": \"SUBSCRIBE\", " +
                        "\"params\": [\"" + depthStream + "\", \"" + tradeStream + "\"], " +
                        "\"id\": 2}";
                try {
                    session.sendMessage(new TextMessage(subscriptionMessage));
                } catch (Exception e) {
                    log.error("Failed to subscribe to depth stream for product: {}", identifier, e);
                }
            }
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            String payload = message.getPayload();
            if (isTicker && payload.contains("24hrTicker")) {
                handleTickerMessage(payload);
            } else if (!isTicker) {
                if (payload.contains("lastUpdateId")) {
                    handleDepthMessage(payload, identifier);

                } else if (payload.contains("\"e\":\"trade\"")) {
                    handleDepthMessage(payload, identifier);
                }
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("WebSocket transport error for {}: {}",
                    isTicker ? "ticker" : "product " + identifier, exception);
            if (isTicker) {
                scheduleReconnect();
            } else {
                reconnect(identifier);
            }
        }

//        @Override
//        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
//            log.warn("WebSocket connection closed for {} - {}",
//                    isTicker ? "ticker" : "product " + identifier, closeStatus);
//            if (!disconnecting) { // Only reconnect if the closure was not intentional
//                if (isTicker) {
//                    tickerSession = null;
//                    scheduleReconnect();
//                } else {
//                    productSessions.remove(identifier);
//                    reconnect(identifier);
//                }
//            } else {
//                log.info("Intentional WebSocket closure for {}", isTicker ? "ticker" : "product " + identifier);
//            }
//        }
    }
}