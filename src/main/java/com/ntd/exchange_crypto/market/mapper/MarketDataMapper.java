package com.ntd.exchange_crypto.market.mapper;

import com.ntd.exchange_crypto.market.CandleStick;
import com.ntd.exchange_crypto.market.MarketTrade;
import com.ntd.exchange_crypto.market.OrderBookData;
import com.ntd.exchange_crypto.market.dto.response.CandleStickResponse;
import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;
import com.ntd.exchange_crypto.market.MarketData;
import com.ntd.exchange_crypto.market.dto.response.MarketTradeResponse;
import com.ntd.exchange_crypto.market.dto.response.OrderBookResponse;

//@Mapper(componentModel = "spring")
public interface MarketDataMapper {
    MarketTickerResponse toResponse(MarketData marketData);
    OrderBookResponse toOrderBookResponse(OrderBookData orderBookData);
    CandleStickResponse toCandleStickResponse(CandleStick candleStick);
    MarketTradeResponse toMarketTradeResponse(MarketTrade marketTrade);
}
