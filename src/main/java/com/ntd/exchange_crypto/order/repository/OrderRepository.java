package com.ntd.exchange_crypto.order.repository;

import com.ntd.exchange_crypto.order.dto.response.OrderResponse;
import com.ntd.exchange_crypto.order.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    @Query("SELECT o FROM Order o " +
            "WHERE ((o.getCryptoId = :crypto1 AND o.giveCryptoId = :crypto2) " +
            "    OR (o.getCryptoId = :crypto2 AND o.giveCryptoId = :crypto1)) " +
            "AND o.userId = :userId")
    List<Order> findAllOrdersByPairAndUser(
            String crypto1,
            String crypto2,
            String userId
    );

    @Query("SELECT o FROM Order o " +
            "WHERE ((o.getCryptoId = :crypto1 AND o.giveCryptoId = :crypto2) " +
            "    OR (o.getCryptoId = :crypto2 AND o.giveCryptoId = :crypto1)) " +
            "AND o.userId = :userId " +
            "AND o.status IN ('NEW', 'PENDING', 'PARTIALLY_FILLED')")
    List<Order> findAllOpenOrdersByPairAndAndUser(
            String crypto1,
            String crypto2,
            String userId
    );


    @Query("SELECT o FROM Order o " +
            "WHERE ((o.getCryptoId = :crypto1 AND o.giveCryptoId = :crypto2) " +
            "    OR (o.getCryptoId = :crypto2 AND o.giveCryptoId = :crypto1)) " +
            "AND o.userId = :userId " +
            "AND o.status IN ('CANCELED', 'EXPIRED', 'FILLED')")
    List<Order> findAllOrdersHistoryByPairAndAndUser(
            String crypto1,
            String crypto2,
            String userId
    );


    List<Order> findOrderByUserId(String userId);
    Page<Order> findByUserId(String userId, Pageable pageable);

}
