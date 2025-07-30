package com.ntd.exchange_crypto.websocket.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntd.exchange_crypto.market.MarketDataReceivedEvent;
import com.ntd.exchange_crypto.market.MarketData;
import com.ntd.exchange_crypto.market.OrderBookData;
import com.ntd.exchange_crypto.market.OrderBookReceivedEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BinanceWebSocketService {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private WebSocketSession session;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Set<String> activeDepthSubscriptions = new HashSet<>(); // tracking current subscriptions
    private final List<String> tickerSymbols;


    @NonFinal
    @Value("${app.binance.websocket-url}")
    protected String BINANCE_WEBSOCKET_URL;

    public BinanceWebSocketService(ApplicationEventPublisher eventPublisher,
                                   ObjectMapper objectMapper,
                                   @Value("#{'${app.binance.ticker-symbols}'.split(',')}") List<String> tickerSymbols) {
        this.eventPublisher = eventPublisher;
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
            session = webSocketClient.execute(
                    new BinanceWebSocketHandler(),
                    headers,
                    URI.create(BINANCE_WEBSOCKET_URL)
            ).get();

            // Schedule heartbeat to keep connection alive
            scheduler.scheduleAtFixedRate(this::sendHeartbeat, 30, 30, TimeUnit.SECONDS);

            subscribeToTicker();

        } catch (Exception e) {
            log.error("Failed to connect to Binance WebSocket", e);
            scheduleReconnect();
        }
    }

    private void sendHeartbeat() {
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new PingMessage());
                log.debug("Heartbeat sent to Binance WebSocket");
            } else {
                log.warn("Cannot send heartbeat - WebSocket session is not open");
                scheduleReconnect();
            }
        } catch (Exception e) {
            log.error("Failed to send heartbeat", e);
            scheduleReconnect();
        }
    }

//    private void subscribeToTicker() {
//        try {
//            if (session != null && session.isOpen()) {
//                String tickerStream = "btcusdt@ticker";
//                session.sendMessage(new TextMessage(
//                        "{\"method\": \"SUBSCRIBE\", " +
//                                "\"params\": [\"" + tickerStream + "\"], " +
//                                "\"id\": 1}"
//                ));
//                log.info("Subscribed to ticker stream: {}", tickerStream);
//            } else {
//                log.warn("Cannot subscribe to ticker - WebSocket session is not open");
//            }
//        } catch (Exception e) {
//            log.error("Failed to subscribe to ticker stream", e);
//        }
//    }

    private void subscribeToTicker() {
        try {
            if (session != null && session.isOpen()) {
                List<String> tickerStreams = tickerSymbols.stream()
                        .map(symbol -> "\"" + symbol.trim().toLowerCase() + "@ticker\"")
                        .collect(Collectors.toList());

                String subscriptionMessage = String.format(
                        "{\"method\": \"SUBSCRIBE\", \"params\": [%s], \"id\": 1}",
                        String.join(", ", tickerStreams)
                );

                session.sendMessage(new TextMessage(subscriptionMessage));
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
            if (session != null && session.isOpen()) {
                if (!activeDepthSubscriptions.contains(productId)) {
                    String depthStream = productId.toLowerCase() + "@depth5";
                    session.sendMessage(new TextMessage(
                            "{\"method\": \"SUBSCRIBE\"," +
                                    " \"params\": [\"" + depthStream + "\"]," +
                                    " \"id\": 2}"
                    ));

                    activeDepthSubscriptions.add(productId); // mark activated

                    log.info("Subscribed to depth stream for product: {}", productId);
                } else {
                    log.debug("Already subscribed to depth stream for product: {}", productId);
                }
            } else {
                log.warn("Cannot subscribe to depth - WebSocket session is not open");
            }
        } catch (Exception e) {
            log.error("Failed to subscribe to depth stream for product: {}", productId, e);
        }
    }

    public synchronized void unsubscribeFromDepth(String productId) {
        try {
            if (session != null && session.isOpen()) {
                if (activeDepthSubscriptions.contains(productId)) {
                    String depthStream = productId.toLowerCase() + "@depth5";
                    session.sendMessage(new TextMessage(
                            "{\"method\": \"UNSUBSCRIBE\", " +
                                    "\"params\": [\"" + depthStream + "\"], " +
                                    "\"id\": 3}"
                    ));

                    activeDepthSubscriptions.remove(productId);
                    log.info("Unsubscribed from depth stream for product: {}", productId);
                } else {
                    log.debug("No active subscription to unsubscribe for product: {}", productId);
                }
            } else {
                log.warn("Cannot unsubscribe from depth - WebSocket session is not open");
            }
        } catch (Exception e) {
            log.error("Failed to unsubscribe from depth stream for product: {}", productId, e);
        }
    }

    //    private void handleTickerMessage(String message) {
//        try {
//            JsonNode rootNode = objectMapper.readTree(message);
//            if (rootNode.isArray()) {
//                for (JsonNode ticker : rootNode) {
//                    if ("24hrTicker".equals(ticker.get("e").asText())) {
//                        String productId = ticker.get("s").asText();
//                        BigDecimal price = new BigDecimal(ticker.get("c").asText());
//                        BigDecimal volume24h = new BigDecimal(ticker.get("v").asText());
//                        BigDecimal low24h = new BigDecimal(ticker.get("l").asText());
//                        BigDecimal high24h = new BigDecimal(ticker.get("h").asText());
//
//                        MarketData marketData = MarketData.builder()
//                                .productId(productId)
//                                .price(price)
//                                .volume24h(volume24h)
//                                .low24h(low24h)
//                                .high24h(high24h)
//                                .timestamp(Instant.ofEpochMilli(ticker.get("E").asLong()))
//                                .build();
//
//
//                        eventPublisher.publishEvent(new MarketDataReceivedEvent(marketData));
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.error("Error processing ticker message: {}", message, e);
//        }
//    }
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

    private void handleDepthMessage(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            if ("depthUpdate".equals(rootNode.get("e").asText())) {
                String productId = rootNode.get("s").asText();
                JsonNode bidsNode = rootNode.get("b");
                JsonNode asksNode = rootNode.get("a");

                if (bidsNode != null && bidsNode.isArray()) {
                    for (JsonNode bid : bidsNode) {
                        String priceLevel = bid.get(0).asText();
                        BigDecimal newQuantity = new BigDecimal(bid.get(1).asText());

                        OrderBookData orderBookData = OrderBookData.builder()
                                .productId(productId)
                                .side("bid")
                                .priceLevel(new BigDecimal(priceLevel))
                                .newQuantity(newQuantity)
                                .timestamp(Instant.ofEpochMilli(rootNode.get("E").asLong()))
                                .build();

                        log.debug("Processed bid update for {} at {}", productId, priceLevel);
                        eventPublisher.publishEvent(new OrderBookReceivedEvent(orderBookData));
                    }
                }

                if (asksNode != null && asksNode.isArray()) {
                    for (JsonNode ask : asksNode) {
                        String priceLevel = ask.get(0).asText();
                        BigDecimal newQuantity = new BigDecimal(ask.get(1).asText());

                        OrderBookData orderBookData = OrderBookData.builder()
                                .productId(productId)
                                .side("ask")
                                .priceLevel(new BigDecimal(priceLevel))
                                .newQuantity(newQuantity)
                                .timestamp(Instant.ofEpochMilli(rootNode.get("E").asLong()))
                                .build();

                        log.debug("Processed ask update for {} at {}", productId, priceLevel);
                        eventPublisher.publishEvent(new OrderBookReceivedEvent(orderBookData));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing depth message: {}", message, e);
        }
    }

    private void scheduleReconnect() {
        if (!scheduler.isShutdown()) {
            scheduler.schedule(() -> {
                log.info("Attempting to reconnect to Binance WebSocket");
                connect();
            }, 5, TimeUnit.SECONDS);
        } else {
            log.error("Scheduler is shutdown, cannot schedule reconnect");
        }
    }

    @PreDestroy
    public void disconnect() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                log.error("Error closing WebSocket session", e);
            }
        }
        scheduler.shutdown();
    }

    private class BinanceWebSocketHandler extends TextWebSocketHandler {

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            log.info("Connected to Binance WebSocket");
            BinanceWebSocketService.this.session = session;
            subscribeToTicker();
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            String payload = message.getPayload();
            if (payload.contains("24hrTicker")) {
                handleTickerMessage(payload);
            } else if (payload.contains("depthUpdate")) {
                handleDepthMessage(payload);
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("WebSocket transport error", exception);
            scheduleReconnect();
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
            log.warn("WebSocket connection closed: {}", closeStatus);
            scheduleReconnect();
        }
    }
}