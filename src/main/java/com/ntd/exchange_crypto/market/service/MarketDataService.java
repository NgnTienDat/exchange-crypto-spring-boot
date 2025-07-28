package com.ntd.exchange_crypto.market.service;

import com.ntd.exchange_crypto.market.MarketDataReceivedEvent;
import com.ntd.exchange_crypto.market.MarketDataUpdatedEvent;
import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;
import com.ntd.exchange_crypto.market.mapper.MarketDataMapperMyImpl;
import com.ntd.exchange_crypto.market.MarketData;
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
public class MarketDataService {

    ApplicationEventPublisher eventPublisher;
    MarketDataMapperMyImpl marketDataMapper;

    @EventListener
    @Async("websocketExecutor")
    public void handleMarketDataReceived(MarketDataReceivedEvent event) {
        try {
            MarketData marketData = event.marketData();

            MarketTickerResponse response = marketDataMapper.toResponse(marketData);

            eventPublisher.publishEvent(new MarketDataUpdatedEvent(response));

        } catch (Exception e) {
            log.error("Error processing market data: {}", event, e);
        }
    }

}