package com.ntd.exchange_crypto.order.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ntd.exchange_crypto.order.enums.OrderStatus;
import com.ntd.exchange_crypto.order.enums.Side;
import com.ntd.exchange_crypto.order.enums.TimeInForce;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity(name = "order")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, length = 36)
    String id;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Column(name = "product_id", nullable = false)
    String productId;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 8)
    BigDecimal quantity;

    @Column(name = "price", nullable = false, precision = 19, scale = 8)
    BigDecimal price;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 8)
    BigDecimal totalAmount;

    // Indicates if the order is a maker order (true) or a taker order (false)
    @Column(name = "is_buyer_maker", nullable = false)
    boolean isBuyerMaker;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 10)
    Side type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    OrderStatus status;

    // The total amount filled for this order
    @Column(name = "filled_quantity", nullable = false, precision = 19, scale = 8)
    BigDecimal filledQuantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_in_force", nullable = false, length = 10)
    TimeInForce timeInForce;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }





}