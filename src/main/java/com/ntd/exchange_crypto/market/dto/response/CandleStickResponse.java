package com.ntd.exchange_crypto.market.dto.response;

import com.ntd.exchange_crypto.market.OrderBookEntry;
import lombok.Builder;

import java.util.List;

@Builder
public record CandleStickResponse(
        String productId,
        long timestamp,
        double open,
        double high,
        double low,
        double close,
        double volume,
        double totalVolume,
        boolean isFinal
) {
}

