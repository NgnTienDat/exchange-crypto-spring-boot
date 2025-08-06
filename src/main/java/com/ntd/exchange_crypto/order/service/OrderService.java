package com.ntd.exchange_crypto.order.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntd.exchange_crypto.asset.AssetExternalAPI;
import com.ntd.exchange_crypto.order.dto.request.OrderCreationRequest;
import com.ntd.exchange_crypto.order.dto.response.OrderResponse;
import com.ntd.exchange_crypto.order.enums.OrderStatus;
import com.ntd.exchange_crypto.order.enums.OrderType;
import com.ntd.exchange_crypto.order.enums.Side;
import com.ntd.exchange_crypto.order.enums.TimeInForce;
import com.ntd.exchange_crypto.order.exception.OrderErrorCode;
import com.ntd.exchange_crypto.order.exception.OrderException;
import com.ntd.exchange_crypto.order.model.Order;
import com.ntd.exchange_crypto.order.repository.OrderRepository;
import com.ntd.exchange_crypto.user.UserDTO;
import com.ntd.exchange_crypto.user.UserExternalAPI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;


@Service
@Transactional
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class OrderService {
    RedisTemplate<String, Object> redisTemplate;
    ObjectMapper objectMapper;
    ApplicationEventPublisher applicationEventPublisher;
    UserExternalAPI userExternalAPI;
    OrderRepository orderRepository;
    AssetExternalAPI assetExternalAPI;


    public OrderResponse placeOrder(OrderCreationRequest orderCreationRequest) throws JsonProcessingException {
        log.info("Placing order for request: {}", orderCreationRequest);
        /*
         *  My balance USDT: 50000
         *  My balance BTC: 1
         *
         *
         *  getProductId: BTC
         *  giveProductId: USDT
         *  quantity: 0.01
         *  price: 114300
         *  side: BUY
         *  Total Amount: 1143 -> Lock giveProductId: 1143 USDT
         *
         *
         *  getProductId: USDT
         *  giveProductId: BTC
         *  quantity: 0.1
         *  price: 114300
         *  side: SELL
         *  Total Amount: 11430 -> Lock giveProductId: 0.9 BTC
         *
         *
         *
         * */
        BigDecimal totalAmount = orderCreationRequest.getPrice().multiply(orderCreationRequest.getQuantity());

        // Step 1: Get current user from security context
        UserDTO userDTO = userExternalAPI.getUserLogin();

        // Step 2: Check if the user has sufficient balance for the order
        if (!assetExternalAPI.hasSufficientBalance(orderCreationRequest.getGiveCryptoId(),
                orderCreationRequest.getQuantity())) {
            throw new OrderException(OrderErrorCode.INSUFFICIENT_BALANCE);
        }

        // Step 3: Create the order object
        Order order = Order.builder()
                .userId(userDTO.getId())
                .getCryptoId(orderCreationRequest.getGetCryptoId())
                .giveCryptoId(orderCreationRequest.getGiveCryptoId())
                .quantity(orderCreationRequest.getQuantity())
                .price(orderCreationRequest.getPrice())
                .side(Side.valueOf(orderCreationRequest.getSide().toUpperCase()))
                .type(OrderType.valueOf(orderCreationRequest.getOrderType().toUpperCase()))
                .status(OrderStatus.OPEN)
                .timeInForce(TimeInForce.valueOf(orderCreationRequest.getTimeInForce().toUpperCase()))
                .filledQuantity(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        try {
            // Step 4: Lock quantity in the user's asset
            assetExternalAPI.lockBalance(orderCreationRequest.getGiveCryptoId(), totalAmount);

            // Step 5: Save the order to the database
            order = orderRepository.save(order);

            // Step 6: Publish the order to Redis
            redisTemplate.opsForHash().put("order:" + order.getId(),
                    "order",
                    objectMapper.writeValueAsString(order));

            // Step 7: send the order to the Redis channel
            redisTemplate.convertAndSend("order:" + orderCreationRequest, order.getId());

        } catch (Exception e) {
            assetExternalAPI.unlockBalance(orderCreationRequest.getGiveCryptoId(), totalAmount);
        }

        // Step 8: Return the order response


        return OrderResponse.builder()
                .pairId(orderCreationRequest.toString())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .createdAt(String.valueOf(order.getCreatedAt()))
                .updatedAt(String.valueOf(order.getUpdatedAt()))
                .side(order.getSide().name())
                .type(order.getType().name())
                .status(order.getStatus().name())
                .filledQuantity(order.getFilledQuantity())
                .build();


    }
}
