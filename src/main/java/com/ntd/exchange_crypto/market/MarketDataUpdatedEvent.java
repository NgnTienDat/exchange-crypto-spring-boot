package com.ntd.exchange_crypto.market;

import com.ntd.exchange_crypto.common.WebSocketMessageEvent;
import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;

public class MarketDataUpdatedEvent extends WebSocketMessageEvent {

    public MarketDataUpdatedEvent(MarketTickerResponse marketData) {
        super("/topic/market/" + marketData.productId(), marketData);
    }
}
