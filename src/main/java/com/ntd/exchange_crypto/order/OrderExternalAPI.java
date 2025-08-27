package com.ntd.exchange_crypto.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ntd.exchange_crypto.common.PagedResponse;
import com.ntd.exchange_crypto.common.SliceResponse;
import com.ntd.exchange_crypto.order.dto.request.OrderCreationRequest;
import com.ntd.exchange_crypto.order.dto.response.OrderResponse;
import com.ntd.exchange_crypto.order.dto.response.OrderStatResponse;
import com.ntd.exchange_crypto.order.enums.Side;
import com.ntd.exchange_crypto.order.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.util.List;

public interface OrderExternalAPI {
    OrderResponse placeOrder(OrderCreationRequest orderCreationRequest);
    Order getOrderById(String orderId);
    List<OrderResponse> getOrdersByPairId(String pairId);
    PagedResponse<OrderResponse> getUserOrders(String userId, int page, int size);
    SliceResponse<OrderResponse> getOpenOrders(String pairId, int page, int size);
    SliceResponse<OrderResponse> getOrderHistory(String pairId, int page, int size);
    OrderStatResponse getOrderStats(String userId);

    List<OrderResponse> getAllMyOrders();

    @PreAuthorize("hasRole('ADMIN')")
    PagedResponse<OrderResponse> getAllOrders(int page, int size);

    Order createOrder(Order order);

    Order updateOrder(Order order);

    String getPairId(Side side, String giveCryptoId, String getCryptoId);

    void updateOrderStatus(Order order, BigDecimal matchQuantity, BigDecimal matchPrice);

    void updateOrderInOrderBookRedis(Order order) throws JsonProcessingException;

    BigDecimal getBestPriceForMarket(Order order, String pairId);


}
