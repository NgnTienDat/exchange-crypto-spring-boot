package com.ntd.exchange_crypto.order;

import com.ntd.exchange_crypto.order.enums.Side;
import com.ntd.exchange_crypto.order.model.Order;

import java.math.BigDecimal;

public interface OrderExternalAPI {

    Order getOrderById(String orderId);

    Order createOrder(Order order);

    Order updateOrder(Order order);

    String getPairId(Side side, String giveCryptoId, String getCryptoId);

    void updateOrderStatus(Order order, BigDecimal matchQuantity, BigDecimal matchPrice);
}
