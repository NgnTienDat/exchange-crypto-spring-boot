package com.ntd.exchange_crypto.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntd.exchange_crypto.trade.service.OrderBookStatsMessageSubscriber;
import com.ntd.exchange_crypto.trade.service.OrderMessageSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Sử dụng StringRedisSerializer cho key
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Sử dụng Jackson2JsonRedisSerializer cho value
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        serializer.setObjectMapper(objectMapper);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }





    @Bean
    public RedisMessageListenerContainer container(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter orderListenerAdapter,
            MessageListenerAdapter orderBookStatListenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(orderListenerAdapter, new PatternTopic("order:*"));
        container.addMessageListener(orderBookStatListenerAdapter, new PatternTopic("orderbook-to-stat:*"));
        return container;
    }

    @Bean
    public MessageListenerAdapter orderListenerAdapter(OrderMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber);
    }
//    @Bean
//    public MessageListenerAdapter orderListenerAdapter(OrderMessageSubscriber subscriber,
//                                                       ObjectMapper objectMapper) {
//        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "handleMessage");
//        // Gọi đến method tên "handleMessage"
//        // Cấu hình serializer để nó tự động chuyển đổi message thành đối tượng Order
//        adapter.setSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, Object.class));
//        adapter.afterPropertiesSet();
//        return adapter;
//    }

    @Bean
    public MessageListenerAdapter orderBookStatListenerAdapter(OrderBookStatsMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules();
    }
}
