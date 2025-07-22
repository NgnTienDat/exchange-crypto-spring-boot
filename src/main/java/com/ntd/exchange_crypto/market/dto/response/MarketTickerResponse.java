package com.ntd.exchange_crypto.market.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketTickerResponse(
        String productId,
        BigDecimal price,
        BigDecimal priceChange24h,
        BigDecimal volume24h,
        String trend, // UP, DOWN, STABLE
        Instant timestamp
) {}
