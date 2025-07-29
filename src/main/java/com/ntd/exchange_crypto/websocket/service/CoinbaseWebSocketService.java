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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;


import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CoinbaseWebSocketService {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final List<String> subscribedProducts;
    private WebSocketSession session;
    private final Map<String, Boolean> level2Subscriptions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @NonFinal
    @Value("${app.coinbase.websocket-url}")
    protected String COINBASE_WEBSOCKET_URL;

    public CoinbaseWebSocketService(ApplicationEventPublisher eventPublisher,
                                    ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.subscribedProducts = List.of("BTC-USD", "ETH-USD", "DOGE-USD", "USDT-USD", "XRP-USD");
        // Configure via properties
    }


    @PostConstruct
    @Async("coinbaseExecutor")
    public void connect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.setDefaultMaxTextMessageBufferSize(5 * 1024 * 1024); // 2MB
            container.setDefaultMaxBinaryMessageBufferSize(5 * 1024 * 1024);

            WebSocketClient webSocketClient = new StandardWebSocketClient(container);
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            log.info("URL WS {}", COINBASE_WEBSOCKET_URL);
            session = webSocketClient.execute(
                    new CoinbaseWebSocketHandler(),
                    headers,
                    URI.create(COINBASE_WEBSOCKET_URL)
            ).get();

            // Schedule heartbeat to keep connection alive
            scheduler.scheduleAtFixedRate(this::sendHeartbeat, 30, 30, TimeUnit.SECONDS);

            // Resubscribe to existing level2 subscriptions
            level2Subscriptions.forEach((productId, subscribed) -> {
                if (subscribed) {
                    subscribeToLevel2(productId);
                }
            });

        } catch (Exception e) {
            log.error("Failed to connect to Coinbase WebSocket", e);
            scheduleReconnect();
        }
    }

    private void sendHeartbeat() {
        try {
            if (session != null && session.isOpen()) {
                // Coinbase doesn't require specific heartbeat format
                // Just send a ping frame to keep connection alive
                session.sendMessage(new PingMessage());
                log.debug("Heartbeat sent to Coinbase WebSocket");
            } else {
                log.warn("Cannot send heartbeat - WebSocket session is not open");
                scheduleReconnect();
            }
        } catch (Exception e) {
            log.error("Failed to send heartbeat", e);
            scheduleReconnect();
        }
    }

    private void subscribeToTicker(WebSocketSession session) {
        try {
            Map<String, Object> subscribeMsg = Map.of(
                    "type", "subscribe",
                    "channel", "ticker_batch",
                    "product_ids", subscribedProducts
            );

            String message = objectMapper.writeValueAsString(subscribeMsg);
            session.sendMessage(new TextMessage(message));
            log.info("Subscribed to ticker channel for products: {}", subscribedProducts);

        } catch (Exception e) {
            log.error("Failed to subscribe to ticker channel", e);
        }
    }

    //    public void subscribeToLevel2(String productId) {
//        try {
//            if (session != null && session.isOpen()) {
//                Map<String, Object> subscribeMsg = Map.of(
//                        "type", "subscribe",
//                        "channel", "level2",
//                        "product_ids", List.of(productId)
//                );
//                String message = objectMapper.writeValueAsString(subscribeMsg);
//                session.sendMessage(new TextMessage(message));
//                log.info("Subscribed to level2 channel for product: {}", productId);
//            } else {
//                log.warn("Cannot subscribe to level2 - WebSocket session is not open");
//            }
//        } catch (Exception e) {
//            log.error("Failed to subscribe to level2 channel for product: {}", productId, e);
//        }
//    }
    public synchronized void subscribeToLevel2(String productId) {
        try {
            if (session != null && session.isOpen()) {
                // Added to store subscription state
                level2Subscriptions.put(productId, true);
                Map<String, Object> subscribeMsg = Map.of(
                        "type", "subscribe",
                        "channel", "level2",
                        "product_ids", List.of(productId)
                );
                String message = objectMapper.writeValueAsString(subscribeMsg);
                session.sendMessage(new TextMessage(message));
                log.info("Subscribed to level2 channel for product: {}", productId);
            } else {
                log.warn("Cannot subscribe to level2 - WebSocket session is not open");
            }
        } catch (Exception e) {
            log.error("Failed to subscribe to level2 channel for product: {}", productId, e);
        }
    }

    private void handleTickerMessage(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            if (rootNode.has("channel") && "ticker_batch".equals(rootNode.get("channel").asText())) {
                JsonNode eventsNode = rootNode.get("events");
                if (eventsNode != null && eventsNode.isArray()) {
                    for (JsonNode event : eventsNode) {
                        JsonNode tickersNode = event.get("tickers");
                        if (tickersNode != null && tickersNode.isArray()) {
                            for (JsonNode ticker : tickersNode) {
                                String productId = ticker.get("product_id").asText();
                                BigDecimal price = new BigDecimal(ticker.get("price").asText());
                                BigDecimal volume24h = new BigDecimal(ticker.get("volume_24_h").asText());
                                BigDecimal low24h = new BigDecimal(ticker.get("low_24_h").asText());
                                BigDecimal high24h = new BigDecimal(ticker.get("high_24_h").asText());
                                BigDecimal low52w = new BigDecimal(ticker.get("low_52_w").asText());
                                BigDecimal high52w = new BigDecimal(ticker.get("high_52_w").asText());
                                BigDecimal priceChangePercent24h = new BigDecimal(ticker.get("price_percent_chg_24_h").asText());

                                MarketData marketData = MarketData.builder()
                                        .productId(productId)
                                        .price(price)
                                        .volume24h(volume24h)
                                        .low24h(low24h)
                                        .high24h(high24h)
                                        .low52w(low52w)
                                        .high52w(high52w)
                                        .priceChangePercent24h(priceChangePercent24h)
                                        .timestamp(Instant.now()) // hoặc parse từ rootNode.get("timestamp")
                                        .build();

                                log.debug("Processed ticker for {}: {}", productId, price);
                                eventPublisher.publishEvent(new MarketDataReceivedEvent(marketData));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing ticker message: {}", message, e);
        }
    }

    private void handleLevel2Message(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            if (rootNode.has("channel") && "l2_data".equals(rootNode.get("channel").asText())) {
                String timestamp = rootNode.get("timestamp").asText();
                String productId = rootNode.get("events").get(0).get("product_id").asText();
                JsonNode updatesNode = rootNode.get("events").get(0).get("updates");

                if (updatesNode != null && updatesNode.isArray()) {
                    for (JsonNode update : updatesNode) {
                        String side = update.get("side").asText();
                        String priceLevel = update.get("price_level").asText();
                        BigDecimal newQuantity = new BigDecimal(update.get("new_quantity").asText());

                        OrderBookData orderBookData = OrderBookData.builder()
                                .productId(productId)
                                .side(side)
                                .priceLevel(new BigDecimal(priceLevel))
                                .newQuantity(newQuantity)
                                .timestamp(Instant.parse(timestamp))
                                .build();

                        log.debug("Processed level2 update for {}: {} at {}", productId, side, priceLevel);
                        eventPublisher.publishEvent(new OrderBookReceivedEvent(orderBookData));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing level2 message: {}", message, e);
        }
    }

    private void scheduleReconnect() {
        if (!scheduler.isShutdown()) { // Kiểm tra trạng thái scheduler
            scheduler.schedule(() -> {
                log.info("Attempting to reconnect to Coinbase WebSocket");
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

    private class CoinbaseWebSocketHandler extends TextWebSocketHandler {

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            log.info("Connected to Coinbase WebSocket");
            CoinbaseWebSocketService.this.session = session;
            subscribeToTicker(session);
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            String payload = message.getPayload();
            if (payload.contains("ticker_batch")) {
                handleTickerMessage(payload);
            } else if (payload.contains("l2_data")) {
                handleLevel2Message(payload);
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




