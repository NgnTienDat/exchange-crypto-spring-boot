package com.ntd.exchange_crypto.trade.mapper;

import com.ntd.exchange_crypto.trade.dto.response.TradeResponse;
import com.ntd.exchange_crypto.trade.model.Trade;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TradeMapper {
    TradeResponse toTradeResponse(Trade trade);

}
