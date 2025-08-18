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
import com.ntd.exchange_crypto.order.mapper.OrderMapper;
import com.ntd.exchange_crypto.order.model.Order;
import com.ntd.exchange_crypto.order.repository.OrderRepository;
import com.ntd.exchange_crypto.trade.OrderBookStatsService;
import com.ntd.exchange_crypto.trade.model.OrderBookStats;
import com.ntd.exchange_crypto.user.UserDTO;
import com.ntd.exchange_crypto.user.UserExternalAPI;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Transactional
public class OrderService implements OrderExternalAPI, OrderInternalAPI {
    RedisTemplate<String, Object> redisTemplate;
    ObjectMapper objectMapper;
    ApplicationEventPublisher applicationEventPublisher;
    UserExternalAPI userExternalAPI;
    OrderRepository orderRepository;
    AssetExternalAPI assetExternalAPI;
    OrderBookStatsService orderBookStatsService;
    OrderMapper orderMapper;

    @Override
    public OrderResponse placeOrder(OrderCreationRequest orderCreationRequest) {
        log.info("Placing order for request: {}", orderCreationRequest);


        // Step 1: Get current user from security context
        UserDTO userDTO = userExternalAPI.getUserLogin();

        // Step 2: Check if the user has sufficient balance for the order
        if (!assetExternalAPI.hasSufficientBalance(orderCreationRequest.getGiveCryptoId(),
                orderCreationRequest.getQuantity())) {
            throw new OrderException(OrderErrorCode.INSUFFICIENT_BALANCE);
        }

        // Step 3: Create the order object
//        Order order = orderMapper.toOrder(orderCreationRequest);
//        order.setStatus(OrderStatus.NEW);
//        order.setUserId(userDTO.getId());


        Order order = Order.builder()
                .userId(userDTO.getId())
                .getCryptoId(orderCreationRequest.getGetCryptoId())
                .giveCryptoId(orderCreationRequest.getGiveCryptoId())
                .quantity(orderCreationRequest.getQuantity())
                .price(orderCreationRequest.getPrice())
                .side(Side.valueOf(orderCreationRequest.getSide().toUpperCase()))
                .type(OrderType.valueOf(orderCreationRequest.getOrderType().toUpperCase()))
                .status(OrderStatus.NEW)
                .timeInForce(TimeInForce.valueOf(orderCreationRequest.getTimeInForce().toUpperCase()))
                .filledQuantity(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        String pairId = this.getPairIdFromOrderBookData(order.getSide(), order.getGiveCryptoId(), order.getGetCryptoId());

        BigDecimal bestPrice = getBestPriceForMarket(order, pairId);

        System.out.println("🔥 PlaceOrder() Best price for " + pairId + ": " + bestPrice);


        BigDecimal amountToLock = order.getSide().equals(Side.BID) ?
                bestPrice.multiply(order.getQuantity()) : order.getQuantity();
        try {

            // Step 4: Lock quantity in the user's asset
            assetExternalAPI.lockBalance(orderCreationRequest.getGiveCryptoId(), amountToLock);

            // Step 5: Save the order to the database
            order = orderRepository.save(order);


            Order finalOrder = order;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    // Step 6: Publish the order to Redis order book
                    try {
                        addOrderToOrderBook(finalOrder);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                    // Step 7: send the order to the Redis channel
                    redisTemplate.convertAndSend("order:" + pairId, finalOrder);
                }
            });


        } catch (Exception e) {
            log.error("Error placing order: {}", e.getMessage(), e);
            assetExternalAPI.unlockBalance(order.getUserId(), order.getGiveCryptoId(), amountToLock);
        }

        return OrderResponse.builder()
                .id(order.getId())
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

    @Override
    public String getPairId(Side side, String giveCryptoId, String getCryptoId) {
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
        double timePart = createdAt.toEpochMilli() / 1e13;
        System.out.println("TIME-PART: " + timePart);// để không ảnh hưởng nhiều đến price
        return pricePart + timePart;
    }


    /* --------------------------------------- External --------------------------------------- */


    @Override
    public void updateOrderStatus(Order orderUpdate, BigDecimal matchQuantity, BigDecimal matchPrice) {
        if (orderUpdate.getStatus() == OrderStatus.PENDING) {
            Order order = this.orderRepository.findById(orderUpdate.getId())
                    .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));
            order.setStatus(orderUpdate.getStatus());
            order.setUpdatedAt(Instant.now());
            orderRepository.saveAndFlush(order);
            this.updateOrderInOrderBookRedis(order);
            log.info("Order {} updated to status {}", order.getId(), order.getStatus());
            orderRepository.save(order);
            return;
        }

        BigDecimal amountToUnlock = orderUpdate.getSide().equals(Side.BID)
                ? matchPrice.multiply(matchQuantity)
                : orderUpdate.getQuantity();


        Order order = getOrderById(orderUpdate.getId());
        order.setStatus(orderUpdate.getStatus());
        order.setFilledQuantity(order.getFilledQuantity().add(matchQuantity));
        order.setUpdatedAt(Instant.now());

        // FILLED
        // c8d: admin                        BID: Maker
        // BTC: 5, USDT: 1000000            -> BTC: 5.1, USDT: 1000000-120500*0.1 = 987950
        // BID - BUY (get BTC - give USDT): 0.1 : 120500 -> locked USDT: 0.1 * 120500 = 12050


        // 121: user                         ASK: Taker
        // BTC: 1, USDT: 1000000            -> BTC: 0.9, USDT: 1000000+120500*0.1 = 1012050
        // ASK - SELL (give BTC - get USDT): 0.1 : 120500 -> locked BTC: 0.1


        // PARTIALLY FILLED
        // c8d: admin
        // BTC: 5.2, USDT: 987950            -> BTC: 5.3, USDT: 987950-121500*0.1 = 975800 (FILLED)
        //                                   -> BTC: 5.25, USDT: 987950-121500*0.05 = 981875 (PARTIALLY FILLED)
        // BID - BUY (get BTC - give USDT): 0.1 : 121500 -> locked USDT: 0.1 * 121500 = 12150
        // BID - BUY (get BTC - give USDT): 0.1 : 121500 -> locked USDT: 0.05 * 121500 = 6075


        // 121: user
        // BTC: 0.9, USDT: 1012050            -> BTC: 0.85, USDT: 1012050+121500*0.05 = 1018125 (FILLED)
        // ASK - SELL (give BTC - get USDT): 0.05 : 121500 -> locked BTC: 0.05

        assetExternalAPI.unlockBalance(orderUpdate.getUserId(), orderUpdate.getGiveCryptoId(), amountToUnlock);


        if (orderUpdate.getSide() == Side.BID) {
            assetExternalAPI.updateAsset(
                    orderUpdate.getUserId(),
                    orderUpdate.getGetCryptoId(),
                    matchQuantity);    // +0.1
            assetExternalAPI.updateAsset(
                    orderUpdate.getUserId(),
                    orderUpdate.getGiveCryptoId(),
                    amountToUnlock.negate());  // -12050
        } else {
            //   ASK: get USDT - give BTC
            assetExternalAPI.updateAsset(
                    orderUpdate.getUserId(),
                    orderUpdate.getGetCryptoId(),
                    matchQuantity.multiply(matchPrice));    // +12050
            assetExternalAPI.updateAsset(
                    orderUpdate.getUserId(),
                    orderUpdate.getGiveCryptoId(),
                    amountToUnlock.negate());  // -0.1

        }

        orderRepository.save(order);
        log.info("Order updated: {}", order);


        // Nếu FILLED, kiểm tra và unlock bất kỳ dư thừa nào
        if (order.getStatus() == OrderStatus.FILLED) {
            BigDecimal initialLockedAmount = order.getPrice().multiply(order.getQuantity()); // Lưu locked amount ban đầu
            if (order.getSide() == Side.BID && initialLockedAmount.compareTo(amountToUnlock) > 0) {
                BigDecimal remainingLocked = initialLockedAmount.subtract(amountToUnlock);
                assetExternalAPI.unlockBalance(orderUpdate.getUserId(), orderUpdate.getGiveCryptoId(), remainingLocked);
            }
        }
        this.updateOrderInOrderBookRedis(order);
    }

    @Override
    public void updateOrderInOrderBookRedis(Order order) {
        String hashKey = "order:" + order.getId();
        String zsetKey = "orderbook:" +
                getPairId(order.getSide(), order.getGiveCryptoId(), order.getGetCryptoId()) +
                ":" + order.getSide().name().toLowerCase();

        if (order.getStatus() == OrderStatus.FILLED
                || order.getStatus() == OrderStatus.CANCELED
                || order.getStatus() == OrderStatus.EXPIRED) {

            // Xóa khỏi Redis
            redisTemplate.delete(hashKey);
            redisTemplate.opsForZSet().remove(zsetKey, order.getId());

        } else if (order.getStatus() == OrderStatus.PARTIALLY_FILLED) {
            BigDecimal remainingQuantity = order.getQuantity().subtract(order.getFilledQuantity());

            Order redisOrder = new Order();
            try {
                BeanUtils.copyProperties(order, redisOrder);
            } catch (Exception e) {
                throw new RuntimeException("Copy order failed", e);
            }
            redisOrder.setQuantity(remainingQuantity);

            try {
                redisTemplate.opsForHash().put(hashKey, "order", objectMapper.writeValueAsString(redisOrder));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            // Cập nhật lại thông tin order trong Redis
            try {
                redisTemplate.opsForHash().put(hashKey, "order", objectMapper.writeValueAsString(order));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

    }


    @Override
    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));
    }

    @Override
    public List<OrderResponse> getOrdersByPairId(String pairId) {
        String baseSymbol = pairId.split("-")[0];
        String quoteSymbol = pairId.split("-")[1];

        UserDTO userDTO = userExternalAPI.getUserLogin();
        String userId = userDTO.getId();

        List<Order> orders = orderRepository.findAllOrdersByPairAndUser(baseSymbol, quoteSymbol, userId);
        if (orders == null) throw new OrderException(OrderErrorCode.ORDER_NOT_FOUND);
        return orders.stream().map(order -> orderMapper.toOrderResponse(order, pairId)).toList();
    }

    @Override
    public List<OrderResponse> getOpenOrders(String pairId) {
        String baseSymbol = pairId.split("-")[0];
        String quoteSymbol = pairId.split("-")[1];

        UserDTO userDTO = userExternalAPI.getUserLogin();
        String userId = userDTO.getId();

        List<Order> orders = orderRepository.findAllOpenOrdersByPairAndAndUser(baseSymbol, quoteSymbol, userId);
        if (orders == null) throw new OrderException(OrderErrorCode.ORDER_NOT_FOUND);
        return orders.stream().map(order -> orderMapper.toOrderResponse(order, pairId)).toList();
    }

    @Override
    public List<OrderResponse> getOrderHistory(String pairId) {
        String baseSymbol = pairId.split("-")[0];
        String quoteSymbol = pairId.split("-")[1];

        UserDTO userDTO = userExternalAPI.getUserLogin();
        String userId = userDTO.getId();

        List<Order> orders = orderRepository.findAllOrdersHistoryByPairAndAndUser(baseSymbol, quoteSymbol, userId);
        if (orders == null) throw new OrderException(OrderErrorCode.ORDER_NOT_FOUND);
        return orders.stream().map(order -> orderMapper.toOrderResponse(order, pairId)).toList();
    }

    @Override
    public List<OrderResponse> getAllMyOrders() {
        return List.of();
    }

    @Override
    public Order createOrder(Order order) {
        return null;
    }

    @Override
    public Order updateOrder(Order orderUpdate) {
        Order order = this.orderRepository.findById(orderUpdate.getId())
                .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));
        order.setStatus(orderUpdate.getStatus());
        order.setUpdatedAt(Instant.now());
        orderRepository.saveAndFlush(order);
        this.updateOrderInOrderBookRedis(order);
        log.info("Order {} updated to status {}", order.getId(), order.getStatus());
        return orderRepository.save(order);
    }

    /* --------------------------------------- Internal --------------------------------------- */

    private String getPairIdFromOrderBookData(Side side, String giveCryptoId, String getCryptoId) {
        return side == Side.BID ?
                getCryptoId + giveCryptoId :
                giveCryptoId + getCryptoId;
    }

    @Override
    public BigDecimal getBestPriceForMarket(Order order, String pairId) {
        Side side = order.getSide();
        OrderBookStats stats = orderBookStatsService.getStats(pairId);
        if (stats == null) {
            log.warn("No order book (Form Binance) stats available for {}", pairId);
            return null;
        }

        return (side == Side.BID) ? stats.getMinAskPrice() : stats.getMaxBidPrice();
    }

}
