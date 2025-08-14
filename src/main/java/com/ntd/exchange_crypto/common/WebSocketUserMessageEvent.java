package com.ntd.exchange_crypto.common;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class WebSocketUserMessageEvent {
    String userId;
    String destination;
    Object payload;
    Instant timestamp;

    public WebSocketUserMessageEvent(String userId, String destination, Object payload) {
        this.userId = userId;
        this.destination = destination;
        this.payload = payload;
        this.timestamp = Instant.now();
    }
}