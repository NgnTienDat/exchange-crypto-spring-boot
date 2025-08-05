package com.ntd.exchange_crypto.order.repository;

import com.ntd.exchange_crypto.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
}
