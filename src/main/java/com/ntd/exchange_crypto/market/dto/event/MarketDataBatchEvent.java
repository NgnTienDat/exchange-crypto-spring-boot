package com.ntd.exchange_crypto.market.dto.event;

import com.ntd.exchange_crypto.common.websocket.event.WebSocketMessageEvent;
import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;

import java.util.List;

public class MarketDataBatchEvent extends WebSocketMessageEvent {

    public MarketDataBatchEvent(List<MarketTickerResponse> marketDataList) {
        super("/topic/market/batch", marketDataList);
    }
}