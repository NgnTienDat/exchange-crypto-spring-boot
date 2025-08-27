package com.ntd.exchange_crypto.order.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ntd.exchange_crypto.common.PagedResponse;
import com.ntd.exchange_crypto.common.SliceResponse;
import com.ntd.exchange_crypto.common.dto.response.APIResponse;
import com.ntd.exchange_crypto.order.OrderExternalAPI;
import com.ntd.exchange_crypto.order.dto.request.OrderCreationRequest;
import com.ntd.exchange_crypto.order.dto.response.AdminOrderBookResponse;
import com.ntd.exchange_crypto.order.dto.response.OrderResponse;
import com.ntd.exchange_crypto.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<APIResponse<List<OrderResponse>>> getOrdersByPairId(
            @PathVariable("productId") String productId) {
        log.info("Fetching orders for productId: {}", productId);
        List<OrderResponse> orders = orderService.getOrdersByPairId(productId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(buildResponse(orders, "Fetched orders by pairId successfully", HttpStatus.OK));
    }

    @GetMapping("/open/{productId}")
    public ResponseEntity<APIResponse<SliceResponse<OrderResponse>>> getOpenOrdersByPairId(
            @PathVariable("productId") String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching orders for productId: {}", productId);
        SliceResponse<OrderResponse> orders = orderService.getOpenOrders(productId, page, size);
        return ResponseEntity.status(HttpStatus.OK)
                .body(buildResponse(orders, "Fetched orders by pairId successfully", HttpStatus.OK));
    }

    @GetMapping("/history/{productId}")
    public ResponseEntity<APIResponse<SliceResponse<OrderResponse>>> getOrderHistoryByPairId(
            @PathVariable("productId") String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching orders for productId: {}", productId);
        SliceResponse<OrderResponse> orders = orderService.getOrderHistory(productId, page, size);
        return ResponseEntity.status(HttpStatus.OK)
                .body(buildResponse(orders, "Fetched orders by pairId successfully", HttpStatus.OK));
    }

    @GetMapping("/")
    public ResponseEntity<APIResponse<List<OrderResponse>>> getAllMyOrders() {

        List<OrderResponse> orders = orderService.getAllMyOrders();
        return ResponseEntity.status(HttpStatus.OK)
                .body(buildResponse(orders, "Fetched all orders successfully", HttpStatus.OK));
    }


    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<PagedResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        PagedResponse<OrderResponse> orders = orderService.getAllOrders(page, size);

        return ResponseEntity.status(HttpStatus.OK)
                .body(buildResponse(orders, "Fetched all orders successfully", HttpStatus.OK));
    }


    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<PagedResponse<OrderResponse>>> getUserOrders(
            @PathVariable("userId") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PagedResponse<OrderResponse> orders = orderService.getUserOrders(userId, page, size);

        return ResponseEntity.status(HttpStatus.OK)
                .body(buildResponse(orders, "Fetched user's orders successfully", HttpStatus.OK));
    }

    @GetMapping("/stats/{userId}")
    public ResponseEntity<APIResponse<?>> getOrderStats(
            @PathVariable("userId") String userId
    ) {
        var stats = orderService.getOrderStats(userId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(buildResponse(stats, "Fetched user's order stats successfully", HttpStatus.OK));
    }


    @GetMapping("/admin/order-book")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<?>> getAdminOrderBook(
            @RequestParam String pairId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        AdminOrderBookResponse orderBook = orderService.getAdminOrderBook(pairId, limit);
        return ResponseEntity.status(HttpStatus.OK)
                .body(buildResponse(orderBook, "Fetched admin order book successfully", HttpStatus.OK));
    }

}
