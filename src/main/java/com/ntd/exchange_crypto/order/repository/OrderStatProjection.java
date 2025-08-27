package com.ntd.exchange_crypto.order.repository;

public interface OrderStatProjection {
    Long getTotalOrder();
    Long getActiveOrder();
    Long getCompleteTrades();
}
