package com.ntd.exchange_crypto.market.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record MarketTickerResponse(
        String productId,
        BigDecimal price,
        BigDecimal volume24h,
        BigDecimal low24h,
        BigDecimal high24h,
        BigDecimal low52w,
        BigDecimal high52w,
        BigDecimal priceChangePercent24h,
        String trend, // "UP", "DOWN", "STABLE"
        Instant timestamp
) {}
