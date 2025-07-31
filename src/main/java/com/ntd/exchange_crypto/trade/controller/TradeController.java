package com.ntd.exchange_crypto.trade.controller;

import com.ntd.exchange_crypto.common.dto.response.APIResponse;
import com.ntd.exchange_crypto.websocket.service.BinanceWebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
public class TradeController {

    private final BinanceWebSocketService binanceWebSocketService;

    public TradeController(BinanceWebSocketService binanceWebSocketService) {
        this.binanceWebSocketService = binanceWebSocketService;
    }

    private <T> APIResponse<T> buildResponse(T result, String message, HttpStatus status) {
        return APIResponse.<T>builder()
                .success(true)
                .code(status.value())
                .message(message)
                .result(result)
                .build();
    }


    @PostMapping("/trade/{productId}")
    public ResponseEntity<APIResponse<?>> subscribeToDepth(@PathVariable("productId") String productId) {
        try {
            log.info("Received request to subscribe to depth5 for product: {}", productId);

            binanceWebSocketService.subscribeToDepth(productId);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(buildResponse(null,
                            "Subscribed to depth5 for " +
                                    productId, HttpStatus.OK));

        } catch (Exception e) {

            log.error("Failed to subscribe to depth5 for product {}: {}", productId, e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildResponse(null,
                            "Failed to subscribe: " + e.getMessage(),
                            HttpStatus.BAD_REQUEST));
        }
    }
}