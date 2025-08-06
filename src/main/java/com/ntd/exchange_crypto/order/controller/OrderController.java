package com.ntd.exchange_crypto.order.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ntd.exchange_crypto.common.dto.response.APIResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderController {

    OrderService orderService;

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
        OrderResponse orderResponse = orderService.placeOrder(orderCreationRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildResponse(orderResponse, "Order created successfully", HttpStatus.CREATED));
    }
}
