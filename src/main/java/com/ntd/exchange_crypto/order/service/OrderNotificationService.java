package com.ntd.exchange_crypto.order.service;

import com.ntd.exchange_crypto.order.OrderDTO;
import com.ntd.exchange_crypto.order.OrderReceivedEvent;
import com.ntd.exchange_crypto.order.OrderUpdatedEvent;
import com.ntd.exchange_crypto.order.dto.response.OrderResponse;
import com.ntd.exchange_crypto.order.mapper.OrderMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderNotificationService {

    ApplicationEventPublisher eventPublisher;


    @EventListener
    @Async("websocketExecutor")
    public void handleMatchOrderReceived(OrderReceivedEvent event) {
        try {
            OrderDTO order = event.orderDTO();
            eventPublisher.publishEvent(new OrderUpdatedEvent(order));
        } catch (Exception e) {
            log.error("Error processing new order notification: {}", event, e);
        }
    }
}
