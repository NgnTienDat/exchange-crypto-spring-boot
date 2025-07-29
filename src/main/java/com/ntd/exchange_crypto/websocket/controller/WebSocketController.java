package com.ntd.exchange_crypto.websocket.controller;

import com.ntd.exchange_crypto.websocket.service.CoinbaseWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class WebSocketController {

    private final CoinbaseWebSocketService coinbaseWebSocketService;

    @PostMapping("/trade/{productId}")
    public ResponseEntity<String> subscribeToLevel2(@PathVariable("productId") String productId) {
        try {
            log.info("Received request to subscribe to level2 for product: {}", productId);
            coinbaseWebSocketService.subscribeToLevel2(productId);
            return ResponseEntity.ok("Subscribed to level2 for " + productId);
        } catch (Exception e) {
            log.error("Failed to subscribe to level2 for product {}: {}", productId, e.getMessage());
            return ResponseEntity.status(500).body("Failed to subscribe: " + e.getMessage());
        }
    }
}