package com.ntd.exchange_crypto.common.websocket.event;

import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class WebSocketMessageEvent {
    String destination;
    Object payload;
    Instant timestamp;

    public WebSocketMessageEvent(String destination, Object payload) {
        this.destination = destination;
        this.payload = payload;
        this.timestamp = Instant.now();
    }
}
