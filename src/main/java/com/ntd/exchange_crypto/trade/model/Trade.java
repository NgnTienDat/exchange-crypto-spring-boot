package com.ntd.exchange_crypto.trade.model;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "trades")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, length = 36)
    String id;

    @Column(name = "buy_order_id", nullable = false)
    String buyOrderId;

    @Column(name = "sell_order_id", nullable = false)
    String sellOrderId;

//    @Column(name = "buyer_user_id", nullable = false)
//    String buyerUserId;
//
//    @Column(name = "seller_user_id", nullable = false)
//    String sellerUserId;

    @Column(name = "product_id", nullable = false)
    String productId;

    @Column(name = "price", nullable = false, precision = 18, scale = 8)
    BigDecimal price;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 8)
    BigDecimal quantity;



//    @Column(name = "is_buyer_maker", nullable = false)
//    boolean isBuyerMaker;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}