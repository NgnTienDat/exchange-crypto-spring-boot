package com.ntd.exchange_crypto.market.service;

import com.ntd.exchange_crypto.market.dto.event.MarketDataReceivedEvent;
import com.ntd.exchange_crypto.market.dto.event.MarketDataUpdatedEvent;
import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;
import com.ntd.exchange_crypto.market.mapper.MarketDataMapper;
import com.ntd.exchange_crypto.market.mapper.MarketDataMapperMyImpl;
import com.ntd.exchange_crypto.market.model.MarketData;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MarketDataProcessor {

    ApplicationEventPublisher eventPublisher;
    MarketDataMapperMyImpl marketDataMapper;

    @EventListener
    @Async("websocketExecutor")
    public void handleMarketDataReceived(MarketDataReceivedEvent event) {
        try {
            MarketData marketData = event.marketData();


            // Convert to response DTO
            MarketTickerResponse response = marketDataMapper.toResponse(marketData);

            // Publish WebSocket event
            eventPublisher.publishEvent(new MarketDataUpdatedEvent(response));

        } catch (Exception e) {
            log.error("Error processing market data: {}", event, e);
        }
    }

//    @EventListener
//    @Async("websocketExecutor")
//    public void handleBatchUpdate(List<MarketDataReceivedEvent> events) {
//        // Handle batch processing for better performance
//        List<MarketTickerResponse> responses = events.stream()
//                .map(event -> {
//                    MarketData saved = repository.save(event.marketData());
//                    return mapper.toResponse(saved);
//                })
//                .toList();
//
//        eventPublisher.publishEvent(new MarketDataBatchEvent(responses));
//    }
}