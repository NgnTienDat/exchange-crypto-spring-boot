package com.ntd.exchange_crypto.trade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntd.exchange_crypto.order.model.Order;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;



@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderMessageSubscriber implements MessageListener {

    ObjectMapper objectMapper;
    MatchEngine matchEngine;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            Order order = objectMapper.readValue(body, Order.class);
            matchEngine.processNewOrder(order);
            log.info("Received order: {}", order);
        } catch (Exception e) {
            log.error("Error processing order message", e);
        }
    }
}
















//
//@Component
//@Slf4j
//@RequiredArgsConstructor
//@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//public class OrderMessageSubscriber { // Không cần implements MessageListener nữa
//
//    // Bỏ ObjectMapper vì MessageListenerAdapter đã xử lý giúp bạn
//
//    // Đổi tên method thành handleMessage và nhận trực tiếp đối tượng Order
//    public void handleMessage(Order order) {
//        System.out.println("🔥 Nhận message từ Redis channel: " + order.getGetCryptoId());
//        try {
//            // Xử lý order tại đây
//            log.info("Received order: {}", order);
//        } catch (Exception e) {
//            log.error("Error processing order message", e);
//        }
//    }
//}



