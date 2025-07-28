package com.ntd.exchange_crypto.market.mapper;

import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;
import com.ntd.exchange_crypto.market.MarketData;

//@Mapper(componentModel = "spring")
public interface MarketDataMapper {
    MarketTickerResponse toResponse(MarketData marketData);
}
