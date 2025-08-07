package com.ntd.exchange_crypto.order.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ntd.exchange_crypto.order.enums.OrderStatus;
import com.ntd.exchange_crypto.order.enums.OrderType;
import com.ntd.exchange_crypto.order.enums.Side;
import com.ntd.exchange_crypto.order.enums.TimeInForce;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "orders")
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

    @Column(name = "get_crypto_id", nullable = false)
    String getCryptoId;

    @Column(name = "give_crypto_id", nullable = false)
    String giveCryptoId;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 8)
    BigDecimal quantity;

    @Column(name = "price", nullable = false, precision = 19, scale = 8)
    BigDecimal price;

    // Indicates if the order is a maker order (true) or a taker order (false)
//    @Column(name = "is_buyer_maker", nullable = false)
//    boolean isBuyerMaker;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt = Instant.now();

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt = Instant.now();

    // Bid and Ask
    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 10)
    Side side;

    // The type of the order (e.g., LIMIT, MARKET)
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    OrderType type;

    // The status of the order (e.g., OPEN, PARTIALLY_FILLED, FILLED, CANCELED)
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