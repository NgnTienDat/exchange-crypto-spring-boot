package com.ntd.exchange_crypto.order;

import com.ntd.exchange_crypto.common.WebSocketUserMessageEvent;

public class OrderUpdatedEvent extends WebSocketUserMessageEvent {

    public OrderUpdatedEvent(OrderDTO orderDTO) {
        super(orderDTO.getUserId(), "/order/notification", orderDTO);
    }
}
