package com.ntd.exchange_crypto.websocket.service;

import com.ntd.exchange_crypto.common.WebSocketMessageEvent;
import com.ntd.exchange_crypto.common.WebSocketUserMessageEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WebSocketBroadcastService {
    SimpMessagingTemplate messagingTemplate;


    @EventListener
    @Async("websocketExecutor")
    public void handleWebSocketMessage(WebSocketMessageEvent event) {
        messagingTemplate.convertAndSend(event.getDestination(), event.getPayload());
    }

    @EventListener
    @Async("websocketExecutor")
    public void sendToUser(WebSocketUserMessageEvent event) {
        messagingTemplate.convertAndSendToUser(event.getUserId(), event.getDestination(), event.getPayload());
    }

    public void broadcastToTopic(String topic, Object payload) {
        messagingTemplate.convertAndSend("/topic/" + topic, payload);
    }
}
