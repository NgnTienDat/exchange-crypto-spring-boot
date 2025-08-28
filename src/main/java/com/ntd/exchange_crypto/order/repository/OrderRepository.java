package com.ntd.exchange_crypto.order.repository;

import com.ntd.exchange_crypto.order.dto.response.OrderResponse;
import com.ntd.exchange_crypto.order.enums.OrderStatus;
import com.ntd.exchange_crypto.order.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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
    Slice<Order> findAllOpenOrdersByPairAndAndUser(
            String crypto1,
            String crypto2,
            String userId,
            Pageable pageable
    );


    @Query("SELECT o FROM Order o " +
            "WHERE ((o.getCryptoId = :crypto1 AND o.giveCryptoId = :crypto2) " +
            "    OR (o.getCryptoId = :crypto2 AND o.giveCryptoId = :crypto1)) " +
            "AND o.userId = :userId " +
            "AND o.status IN ('CANCELED', 'EXPIRED', 'FILLED')")
    Slice<Order> findAllOrdersHistoryByPairAndAndUser(
            String crypto1,
            String crypto2,
            String userId,
            Pageable pageable
    );


    List<Order> findOrderByUserId(String userId);

    Page<Order> findByUserId(String userId, Pageable pageable);


    @Query(value = """
                SELECT * FROM orders o
                WHERE ((o.get_crypto_id = ?1 AND o.give_crypto_id = ?2)
                    OR (o.get_crypto_id = ?2 AND o.give_crypto_id = ?1))
                  AND o.side = 'BID'
                  AND o.status IN ('NEW', 'PARTIALLY_FILLED', 'PENDING')
                ORDER BY o.price DESC, o.created_at ASC
                LIMIT ?3
            """, nativeQuery = true)
    List<Order> findBidOrder(String crypto1, String crypto2, int limit);


    @Query(value = """
                SELECT * FROM orders o
                WHERE ((o.get_crypto_id = ?1 AND o.give_crypto_id = ?2)
                    OR (o.get_crypto_id = ?2 AND o.give_crypto_id = ?1))
                  AND o.side = 'ASK'
                  AND o.status IN ('NEW', 'PARTIALLY_FILLED', 'PENDING')
                ORDER BY o.price ASC, o.created_at ASC
                LIMIT ?3
            """, nativeQuery = true)
    List<Order> findAskOrder(String crypto1, String crypto2, int limit);




    @Query(value = """
            SELECT 
                COUNT(*) AS totalOrder,
                SUM(CASE WHEN status IN ('PENDING', 'NEW', 'PARTIALLY_FILLED') THEN 1 ELSE 0 END) AS activeOrder,
                SUM(CASE WHEN status = 'FILLED' THEN 1 ELSE 0 END) AS completeTrades
            FROM orders
            WHERE user_id = :userId
            """, nativeQuery = true)
    OrderStatProjection getOrderStats(String userId);


    // Basic date range queries
    List<Order> findByCreatedAtBetween(Instant startDate, Instant endDate);

    List<Order> findByCreatedAtBetweenAndStatus(Instant startDate, Instant endDate, OrderStatus status);

    List<Order> findByCreatedAtBetweenAndUserId(Instant startDate, Instant endDate, String userId);

    // Custom queries for better performance with large datasets
    @Query("SELECT o FROM Order o WHERE DATE(o.createdAt) = CURRENT_DATE")
    List<Order> findTodayOrders();

    @Query("SELECT o FROM Order o WHERE YEAR(o.createdAt) = :year AND MONTH(o.createdAt) = :month")
    List<Order> findByYearAndMonth(int year, int month);

    @Query("SELECT o FROM Order o WHERE YEAR(o.createdAt) = :year")
    List<Order> findByYear(int year);

    // Statistics queries for better performance
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    long countOrdersBetweenDates(Instant startDate,Instant endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.status = :status")
    long countOrdersByStatusBetweenDates(Instant startDate,
                                          Instant endDate,
                                         OrderStatus status);
}
