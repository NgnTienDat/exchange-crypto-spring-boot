package com.ntd.exchange_crypto.order.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ntd.exchange_crypto.common.dto.response.APIResponse;
import com.ntd.exchange_crypto.order.OrderExternalAPI;
import com.ntd.exchange_crypto.order.dto.request.OrderCreationRequest;
import com.ntd.exchange_crypto.order.dto.response.OrderResponse;
import com.ntd.exchange_crypto.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderController {

    OrderExternalAPI orderService;

    private <T> APIResponse<T> buildResponse(T result, String message, HttpStatus status) {
        return APIResponse.<T>builder()
                .success(true)
                .code(status.value())
                .message(message)
                .result(result)
                .build();
    }

    @PostMapping("/")
    public ResponseEntity<APIResponse<OrderResponse>> createOrder(
            @RequestBody @Valid OrderCreationRequest orderCreationRequest) throws JsonProcessingException {
        log.info("Received order creation request: {}", orderCreationRequest);
        OrderResponse orderResponse = orderService.placeOrder(orderCreationRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildResponse(orderResponse, "Order created successfully", HttpStatus.CREATED));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<APIResponse<List<OrderResponse>>> getOrdersByPairId(@PathVariable("productId") String productId) {
        log.info("Fetching orders for productId: {}", productId);
        List<OrderResponse> orders = orderService.getOrdersByPairId(productId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(buildResponse(orders, "Fetched orders by pairId successfully", HttpStatus.OK));
    }

    @GetMapping("/open/{productId}")
    public ResponseEntity<APIResponse<List<OrderResponse>>> getOpenOrdersByPairId(@PathVariable("productId") String productId) {
        log.info("Fetching orders for productId: {}", productId);
        List<OrderResponse> orders = orderService.getOpenOrders(productId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(buildResponse(orders, "Fetched orders by pairId successfully", HttpStatus.OK));
    }

    @GetMapping("/history/{productId}")
    public ResponseEntity<APIResponse<List<OrderResponse>>> getOrderHistoryByPairId(@PathVariable("productId") String productId) {
        log.info("Fetching orders for productId: {}", productId);
        List<OrderResponse> orders = orderService.getOrderHistory(productId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(buildResponse(orders, "Fetched orders by pairId successfully", HttpStatus.OK));
    }
}
