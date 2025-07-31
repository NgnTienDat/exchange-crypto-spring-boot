package com.ntd.exchange_crypto.market;

import com.ntd.exchange_crypto.common.WebSocketMessageEvent;
import com.ntd.exchange_crypto.market.dto.response.CandleStickResponse;
import com.ntd.exchange_crypto.market.dto.response.OrderBookResponse;

public class CandleStickUpdatedEvent extends WebSocketMessageEvent {

    public CandleStickUpdatedEvent(CandleStickResponse candleStickResponse) {
        super("/topic/kline/" + candleStickResponse.productId(), candleStickResponse);
    }
}
