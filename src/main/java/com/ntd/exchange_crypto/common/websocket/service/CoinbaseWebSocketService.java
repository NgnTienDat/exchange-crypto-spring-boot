package com.ntd.exchange_crypto.common.websocket.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntd.exchange_crypto.market.dto.event.MarketDataReceivedEvent;
import com.ntd.exchange_crypto.market.model.MarketData;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @NonFinal
    @Value("${app.coinbase.websocket-url}")
    protected String COINBASE_WEBSOCKET_URL;

    public CoinbaseWebSocketService(ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.subscribedProducts = List.of("BTC-USD"); // Configure via properties
    }
    //, "ETH-USD", "DOGE-USD", "USDT-USD", "XRP-USD"

    @PostConstruct
    @Async("coinbaseExecutor")
    public void connect() {
        try {
            WebSocketClient webSocketClient = new StandardWebSocketClient();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

            session = webSocketClient.execute(
                    new CoinbaseWebSocketHandler(),
                    headers,
                    URI.create(COINBASE_WEBSOCKET_URL)
            ).get();

            // Schedule heartbeat to keep connection alive
            scheduler.scheduleAtFixedRate(this::sendHeartbeat, 30, 30, TimeUnit.SECONDS);

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
                    "channel", "ticker",
                    "product_ids", subscribedProducts
            );

            String message = objectMapper.writeValueAsString(subscribeMsg);
            session.sendMessage(new TextMessage(message));
            log.info("Subscribed to ticker channel for products: {}", subscribedProducts);

        } catch (Exception e) {
            log.error("Failed to subscribe to ticker channel", e);
        }
    }

//    private void handleTickerMessage(String message) {
//        try {
//            JsonNode jsonNode = objectMapper.readTree(message);
//
//            if ("ticker".equals(jsonNode.get("channel").asText())) {
//                String productId = jsonNode.get("product_id").asText();
//                BigDecimal price = new BigDecimal(jsonNode.get("price").asText());
//                BigDecimal volume24h = new BigDecimal(jsonNode.get("volume_24_h").asText());
//
//                // Calculate price change (simplified - you might want to store previous prices)
//                BigDecimal priceChange24h = BigDecimal.ZERO; // Implement logic
//
//                MarketData marketData = new MarketData();
//                marketData.setProductId(productId);
//                marketData.setPrice(price);
//                marketData.setPriceChange24h(priceChange24h);
//                marketData.setVolume24h(volume24h);
//                marketData.setTimestamp(Instant.now());
//
//                // Publish domain event
//                eventPublisher.publishEvent(new MarketDataReceivedEvent(marketData));
//
//            }
//        } catch (Exception e) {
//            log.error("Error processing ticker message: {}", message, e);
//        }
//    }
// market/service/CoinbaseWebSocketService.java

    private void handleTickerMessage(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);

            // Kiểm tra xem có phải là message từ kênh "ticker" không
            if (rootNode.has("channel") && "ticker".equals(rootNode.get("channel").asText())) {

                // 1. Lấy ra mảng "events"
                JsonNode eventsNode = rootNode.get("events");
                if (eventsNode != null && eventsNode.isArray()) {

                    // 2. Lặp qua từng "event" trong mảng
                    for (JsonNode event : eventsNode) {

                        // 3. Lấy ra mảng "tickers" từ bên trong event
                        JsonNode tickersNode = event.get("tickers");
                        if (tickersNode != null && tickersNode.isArray()) {

                            // 4. Lặp qua từng "ticker" trong mảng
                            for (JsonNode ticker : tickersNode) {

                                // 5. Bây giờ mới lấy dữ liệu từ đối tượng ticker
                                String productId = ticker.get("product_id").asText();
                                BigDecimal price = new BigDecimal(ticker.get("price").asText());
                                BigDecimal volume24h = new BigDecimal(ticker.get("volume_24_h").asText());

                                // Lấy các trường khác nếu cần...
                                // BigDecimal priceChange24h = new BigDecimal(ticker.get("price_percent_chg_24_h").asText());

                                // Tạo đối tượng MarketData
                                MarketData marketData = new MarketData();
                                marketData.setProductId(productId);
                                marketData.setPrice(price);
                                // Tính toán priceChange24h nếu cần
                                marketData.setPriceChange24h(BigDecimal.ZERO); // Tạm thời
                                marketData.setVolume24h(volume24h);
                                marketData.setTimestamp(Instant.now());

                                // Publish domain event
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

    private class CoinbaseWebSocketHandler extends TextWebSocketHandler {

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            log.info("Connected to Coinbase WebSocket");
            CoinbaseWebSocketService.this.session = session;
            subscribeToTicker(session);
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            handleTickerMessage(message.getPayload());
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

    private void scheduleReconnect() {
        scheduler.schedule(() -> {
            log.info("Attempting to reconnect to Coinbase WebSocket");
            connect();
        }, 5, TimeUnit.SECONDS);
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
}




