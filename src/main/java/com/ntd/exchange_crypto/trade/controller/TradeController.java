package com.ntd.exchange_crypto.trade.controller;

import com.ntd.exchange_crypto.common.SliceResponse;
import com.ntd.exchange_crypto.common.dto.response.APIResponse;
import com.ntd.exchange_crypto.order.dto.response.OrderResponse;
import com.ntd.exchange_crypto.trade.TradeExternalApi;
import com.ntd.exchange_crypto.trade.dto.response.TradeResponse;
import com.ntd.exchange_crypto.trade.service.TradeService;
import com.ntd.exchange_crypto.websocket.service.BinanceWebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
public class TradeController {

    private final BinanceWebSocketService binanceWebSocketService;
    private final TradeExternalApi tradeService;

    public TradeController(BinanceWebSocketService binanceWebSocketService, TradeExternalApi tradeService) {
        this.binanceWebSocketService = binanceWebSocketService;
        this.tradeService = tradeService;
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

    @PostMapping("/trade/unsubscribe/{productId}")
    public ResponseEntity<APIResponse<?>> unSubscribeFromDepth(@PathVariable("productId") String productId) {
        try {
            log.info("Received request to UNSUBSCRIBE from depth20, product: {}", productId);

            binanceWebSocketService.unsubscribeFromDepth(productId);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(buildResponse(null,
                            "Unsubscribed from depth20 for " +
                                    productId, HttpStatus.OK));

        } catch (Exception e) {

            log.error("Failed to unsubscribe from depth20 for product {}: {}", productId, e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildResponse(null,
                            "Failed to unsubscribe: " + e.getMessage(),
                            HttpStatus.BAD_REQUEST));
        }
    }

    @GetMapping("/trades")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<SliceResponse<TradeResponse>>> getAllTrades(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        SliceResponse<TradeResponse> trades = tradeService.getAllTradesAdmin(page, size);
        return ResponseEntity.status(HttpStatus.OK)
                .body(buildResponse(trades, "Fetched all trades successfully", HttpStatus.OK));
    }


}