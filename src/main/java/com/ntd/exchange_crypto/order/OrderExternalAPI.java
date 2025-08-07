package com.ntd.exchange_crypto.order;

import com.ntd.exchange_crypto.order.model.Order;

public interface OrderExternalAPI {

    Order getOrderById(String orderId);

    Order createOrder(Order order);

    Order updateOrder(Order order);
}
