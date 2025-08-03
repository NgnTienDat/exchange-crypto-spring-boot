package com.ntd.exchange_crypto.market.dto.response;

import com.ntd.exchange_crypto.market.OrderBookEntry;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record MarketTradeResponse(
        String productId,
        Long tradeId,
        BigDecimal price,
        BigDecimal quantity,
        Long tradeTime,
        boolean isMaker,
        BigDecimal totalValue
) {}

