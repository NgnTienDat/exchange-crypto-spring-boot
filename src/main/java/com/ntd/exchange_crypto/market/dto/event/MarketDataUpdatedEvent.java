package com.ntd.exchange_crypto.market.dto.event;

import com.ntd.exchange_crypto.common.websocket.event.WebSocketMessageEvent;
import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;

public class MarketDataUpdatedEvent extends WebSocketMessageEvent {

    public MarketDataUpdatedEvent(MarketTickerResponse marketData) {
        super("/topic/market/" + marketData.productId(), marketData);
    }
}