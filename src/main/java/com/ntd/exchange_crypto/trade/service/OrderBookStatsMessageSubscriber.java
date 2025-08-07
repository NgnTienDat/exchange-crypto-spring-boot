package com.ntd.exchange_crypto.trade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntd.exchange_crypto.market.OrderBookData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderBookStatsMessageSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final OrderBookStatsService orderBookStatsService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());

            OrderBookData data = objectMapper.readValue(body, OrderBookData.class);
            String productId = data.getProductId();

            orderBookStatsService.updateStats(productId, data);

        } catch (Exception e) {
            log.error("Error handling Redis message", e);
        }
    }
}

//@Component
//public class OrderBookStatsMessageSubscriber implements MessageListener {
//
//    @Override
//    public void onMessage(Message message, byte[] pattern) {
//        String channel = new String(message.getChannel());
//        String body = new String(message.getBody());
//
//        System.out.println("🔥 Nhận message từ Redis channel: " + channel);
//        System.out.println("📦 Nội dung: " + body);
//
//        // Gọi đến match engine hoặc service xử lý
//        // Ví dụ:
//        // tradeService.handleNewOrder(body);
//    }
//}
