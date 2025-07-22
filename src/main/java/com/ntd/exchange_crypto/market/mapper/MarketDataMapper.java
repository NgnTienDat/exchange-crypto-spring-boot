package com.ntd.exchange_crypto.market.mapper;

import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;
import com.ntd.exchange_crypto.market.model.MarketData;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MarketDataMapper {
    MarketTickerResponse toResponse(MarketData marketData);
}
