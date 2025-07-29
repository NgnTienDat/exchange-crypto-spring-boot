package com.ntd.exchange_crypto.market;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderBookData {
    private String productId;
    private String side; // bid & offer
    private BigDecimal priceLevel;
    private BigDecimal newQuantity;
    private Instant timestamp;
}