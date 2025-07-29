package com.ntd.exchange_crypto.market;

import com.ntd.exchange_crypto.common.WebSocketMessageEvent;
import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;
import com.ntd.exchange_crypto.market.dto.response.OrderBookResponse;

public class OrderBookUpdatedEvent extends WebSocketMessageEvent {

    public OrderBookUpdatedEvent(OrderBookResponse orderBookResponse) {
        super("/topic/trade/" + orderBookResponse.productId(), orderBookResponse);
    }
}
