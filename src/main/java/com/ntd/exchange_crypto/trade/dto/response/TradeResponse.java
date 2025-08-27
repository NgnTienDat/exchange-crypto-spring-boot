package com.ntd.exchange_crypto.trade.dto.response;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TradeResponse {

    String id;

    String takerOrderId;

    String makerOrderId;

    String productId;

    BigDecimal price;

    BigDecimal quantity;

    boolean isBuyerMaker;

    Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}