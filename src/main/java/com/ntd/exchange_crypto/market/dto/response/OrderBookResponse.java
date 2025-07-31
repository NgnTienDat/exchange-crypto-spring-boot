package com.ntd.exchange_crypto.market.dto.response;

import com.ntd.exchange_crypto.market.OrderBookEntry;
import lombok.Builder;

import java.util.List;

@Builder
public record OrderBookResponse(
        String productId,
        List<OrderBookEntry> bids,
        List<OrderBookEntry> asks,
        Long lastUpdateId
) {}

