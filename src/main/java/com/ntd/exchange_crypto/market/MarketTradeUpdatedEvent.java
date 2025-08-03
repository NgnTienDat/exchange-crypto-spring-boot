package com.ntd.exchange_crypto.market;

import com.ntd.exchange_crypto.common.WebSocketMessageEvent;
import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;
import com.ntd.exchange_crypto.market.dto.response.MarketTradeResponse;

public class MarketTradeUpdatedEvent extends WebSocketMessageEvent {

    public MarketTradeUpdatedEvent(MarketTradeResponse marketData) {
        super("/topic/market-trade/" + marketData.productId(), marketData);
    }
}
