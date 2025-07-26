package com.ntd.exchange_crypto.market.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MarketData {
    private String productId;
    private BigDecimal price;
    private BigDecimal volume24h;
    private BigDecimal low24h;
    private BigDecimal high24h;
    private BigDecimal low52w;
    private BigDecimal high52w;
    private BigDecimal priceChangePercent24h;
    private Instant timestamp;
}