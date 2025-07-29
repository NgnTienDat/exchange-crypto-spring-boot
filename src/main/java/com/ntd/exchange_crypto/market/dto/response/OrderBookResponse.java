package com.ntd.exchange_crypto.market.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record OrderBookResponse(
        String productId,
        String side, // bid & offer
        BigDecimal priceLevel,
        BigDecimal newQuantity,
        Instant timestamp
) {}
