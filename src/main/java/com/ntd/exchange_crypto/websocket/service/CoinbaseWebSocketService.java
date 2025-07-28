package com.ntd.exchange_crypto.websocket.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntd.exchange_crypto.market.MarketDataReceivedEvent;
import com.ntd.exchange_crypto.market.MarketData;
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
        this.subscribedProducts = List.of("BTC-USD", "ETH-USD", "DOGE-USD", "USDT-USD", "XRP-USD"); // Configure via properties
    }
    //, "ETH-USD", "DOGE-USD", "USDT-USD", "XRP-USD"

    @PostConstruct
    @Async("coinbaseExecutor")
    public void connect() {
        try {
            WebSocketClient webSocketClient = new StandardWebSocketClient();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            log.info("URL WS {}", COINBASE_WEBSOCKET_URL);
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




