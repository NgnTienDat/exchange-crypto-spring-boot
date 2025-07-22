package com.ntd.exchange_crypto.market.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MarketData {
    private String productId;
    private BigDecimal price;
    BigDecimal priceChange24h;
    private BigDecimal volume24h;
    private Instant timestamp;
}