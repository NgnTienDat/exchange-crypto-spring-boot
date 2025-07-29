package com.ntd.exchange_crypto.websocket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean("websocketExecutor")
    public Executor websocketExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(50); // Tăng maxPoolSize để xử lý tải cao hơn
        executor.setQueueCapacity(1000); // Tăng queueCapacity để chứa nhiều task hơn
        executor.setThreadNamePrefix("WebSocket-");
        executor.setRejectedExecutionHandler((r, e)
                -> log.warn("Task rejected from WebSocketExecutor: {}", r)); // Log khi task bị từ chối
        executor.initialize();
        return executor;
    }

    @Bean("coinbaseExecutor")
    public Executor coinbaseExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("Coinbase-");
        executor.setRejectedExecutionHandler((r, e)
                -> log.warn("Task rejected from CoinbaseExecutor: {}", r));
        executor.initialize();
        return executor;
    }

    // Thêm logging để debug
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AsyncConfig.class);
}