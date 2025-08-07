package com.ntd.exchange_crypto.order.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntd.exchange_crypto.asset.AssetExternalAPI;
import com.ntd.exchange_crypto.order.OrderExternalAPI;
import com.ntd.exchange_crypto.order.OrderInternalAPI;
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
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;


@Service
@Transactional
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class OrderService implements OrderExternalAPI, OrderInternalAPI {
    RedisTemplate<String, Object> redisTemplate;
    ObjectMapper objectMapper;
    ApplicationEventPublisher applicationEventPublisher;
    UserExternalAPI userExternalAPI;
    OrderRepository orderRepository;
    AssetExternalAPI assetExternalAPI;


    public OrderResponse placeOrder(OrderCreationRequest orderCreationRequest) throws JsonProcessingException {
        log.info("Placing order for request: {}", orderCreationRequest);

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

        String pairId = this.getPairId(order.getSide(), order.getGiveCryptoId(), order.getGetCryptoId());



        try {
            BigDecimal amountToLock = order.getSide().equals(Side.BID) ?
                    totalAmount : order.getQuantity();

            // Step 4: Lock quantity in the user's asset
            assetExternalAPI.lockBalance(orderCreationRequest.getGiveCryptoId(), amountToLock);

            // Step 5: Save the order to the database
            order = orderRepository.save(order);

            // Step 6: Publish the order to Redis order book
            this.addOrderToOrderBook(order);

            // Step 7: send the order to the Redis channel
            redisTemplate.convertAndSend("order:" + pairId, order);

        } catch (Exception e) {
            assetExternalAPI.unlockBalance(orderCreationRequest.getGiveCryptoId(), totalAmount);
        }

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

    private String getPairId(Side side, String giveCryptoId, String getCryptoId) {
        return side == Side.BID ?
                getCryptoId + "-" + giveCryptoId :
                giveCryptoId + "-" + getCryptoId;
    }


    private void addOrderToOrderBook(Order order) throws JsonProcessingException {
        // HSET order:<uuid> order <order_json>                        Terminal: // HGET key field
        //          key      field    value
        // ZADD orderbook:<BTC-USDT>:<BUY> <score> order:<uuid>        Terminal: // ZRANGE key start stop WITHSCORES
        //             key                  score   member

        String pairId = getPairId(order.getSide(), order.getGiveCryptoId(), order.getGetCryptoId());
        String hashKey = "order:" + order.getId();
        String zsetKey = buildOrderBookKey(pairId, order.getSide());

        redisTemplate.opsForHash().put(hashKey, "order", objectMapper.writeValueAsString(order));

        double score = computeOrderScore(order.getPrice(), order.getCreatedAt(), order.getSide());

        redisTemplate.opsForZSet().add(zsetKey, order.getId(), score);

    }

    private String buildOrderBookKey(String pairId, Side side) {
        return "orderbook:" + pairId + ":" + side.name().toLowerCase();
    }

    private double computeOrderScore(BigDecimal price, Instant createdAt, Side side) {
        // Với side = BID -> ưu tiên price cao hơn -> đảo dấu để ZSet sắp xếp tăng dần
        // Với side = ASK -> ưu tiên price thấp hơn (Mặc định trong ZSet là tăng dần)
        double pricePart = side == Side.BID ? -price.doubleValue() : price.doubleValue();
        double timePart = createdAt.toEpochMilli() / 1e13; // để không ảnh hưởng nhiều đến price
        return pricePart + timePart;
    }


    /* --------------------------------------- External --------------------------------------- */


    @Override
    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));
    }

    @Override
    public Order createOrder(Order order) {
        return null;
    }

    @Override
    public Order updateOrder(Order order) {
        return null;
    }

    /* --------------------------------------- Internal --------------------------------------- */

}
