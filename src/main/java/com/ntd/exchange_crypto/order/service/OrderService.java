package com.ntd.exchange_crypto.order.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntd.exchange_crypto.common.exception.AppException;
import com.ntd.exchange_crypto.common.exception.ErrorCode;
import com.ntd.exchange_crypto.order.dto.request.OrderCreationRequest;
import com.ntd.exchange_crypto.order.dto.response.OrderResponse;
import com.ntd.exchange_crypto.order.enums.OrderStatus;
import com.ntd.exchange_crypto.order.enums.OrderType;
import com.ntd.exchange_crypto.order.enums.Side;
import com.ntd.exchange_crypto.order.enums.TimeInForce;
import com.ntd.exchange_crypto.order.model.Order;
import com.ntd.exchange_crypto.order.repository.OrderRepository;
import com.ntd.exchange_crypto.user.UserDTO;
import com.ntd.exchange_crypto.user.UserExternalAPI;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;


@Service
@Transactional
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderService {
    RedisTemplate<String, Object> redisTemplate;
    ObjectMapper objectMapper;
    ApplicationEventPublisher applicationEventPublisher;
    UserExternalAPI userExternalAPI;
    OrderRepository orderRepository;

    public OrderService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper, ApplicationEventPublisher applicationEventPublisher, UserExternalAPI userExternalAPI, OrderRepository orderRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.userExternalAPI = userExternalAPI;
        this.orderRepository = orderRepository;
    }

    public OrderResponse placeOrder(OrderCreationRequest orderCreationRequest) throws JsonProcessingException {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        UserDTO userDTO = userExternalAPI.userExistsByEmail(email);

        if (userDTO.getEmail().equals(email))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        Order order = Order.builder()
                .userId(userDTO.getId())
                .productId(orderCreationRequest.getProductId())
                .quantity(orderCreationRequest.getQuantity())
                .price(orderCreationRequest.getPrice())
                .totalAmount(orderCreationRequest.getPrice().multiply(orderCreationRequest.getQuantity()))
                .isBuyerMaker("BUY".equalsIgnoreCase(orderCreationRequest.getSide()))
                .side(Side.valueOf(orderCreationRequest.getSide().toUpperCase()))
                .orderType(OrderType.valueOf(orderCreationRequest.getType().toUpperCase()))
                .timeInForce(TimeInForce.valueOf(orderCreationRequest.getTimeInForce().toUpperCase()))
                .status(OrderStatus.valueOf(orderCreationRequest.getOrderStatus().toUpperCase()))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        order = orderRepository.save(order);
        redisTemplate.opsForHash().put("order:" + order.getId(),
                "order",
                objectMapper.writeValueAsString(order));
        redisTemplate.convertAndSend("order:" + order.getProductId(), order.getId());



    }
}
