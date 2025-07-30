//package com.ntd.exchange_crypto.market.service;
//
//import com.ntd.exchange_crypto.market.*;
//import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;
//import com.ntd.exchange_crypto.market.dto.response.OrderBookResponse;
//import com.ntd.exchange_crypto.market.mapper.MarketDataMapperMyImpl;
//import lombok.AccessLevel;
//import lombok.RequiredArgsConstructor;
//import lombok.experimental.FieldDefaults;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.ApplicationEventPublisher;
//import org.springframework.context.event.EventListener;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@Transactional
//@Slf4j
//@RequiredArgsConstructor
//@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//public class MarketDataService {
//
//    ApplicationEventPublisher eventPublisher;
//    MarketDataMapperMyImpl marketDataMapper;
//
//    @EventListener
//    @Async("websocketExecutor")
//    public void handleMarketDataReceived(MarketDataReceivedEvent event) {
//        try {
//            MarketData marketData = event.marketData();
//
//            MarketTickerResponse response = marketDataMapper.toResponse(marketData);
//
//            eventPublisher.publishEvent(new MarketDataUpdatedEvent(response));
//
//        } catch (Exception e) {
//            log.error("Error processing market data: {}", event, e);
//        }
//    }
//
//    @EventListener
//    @Async("websocketExecutor")
//    public void handleOrderBookReceived(OrderBookReceivedEvent event) {
//        try {
//            OrderBookData orderBookData = event.orderBookData();
//            log.debug("Processing order book update for {}: {} at {}",
//                    orderBookData.getProductId(), orderBookData.getSide(), orderBookData.getPriceLevel());
//
//            OrderBookResponse response = marketDataMapper.toOrderBookResponse(orderBookData);
//
//            eventPublisher.publishEvent(new OrderBookUpdatedEvent(response));
//        } catch (Exception e) {
//            log.error("Error processing order book update: {}", event.orderBookData(), e);
//        }
//    }
//
//}

package com.ntd.exchange_crypto.market.service;

import com.ntd.exchange_crypto.market.*;
import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;
import com.ntd.exchange_crypto.market.dto.response.OrderBookResponse;
import com.ntd.exchange_crypto.market.mapper.MarketDataMapperMyImpl;
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

//            System.out.println("MARKET DATA: "+marketData);

            MarketTickerResponse response = marketDataMapper.toResponse(marketData);
            eventPublisher.publishEvent(new MarketDataUpdatedEvent(response));
        } catch (Exception e) {
            log.error("Error processing market data: {}", event, e);
        }
    }

    @EventListener
    @Async("websocketExecutor")
    public void handleOrderBookReceived(OrderBookReceivedEvent event) {
        try {
            OrderBookData orderBookData = event.orderBookData();
            log.debug("Processing order book update for {}: {} at {}",
                    orderBookData.getProductId(), orderBookData.getSide(), orderBookData.getPriceLevel());

            OrderBookResponse response = marketDataMapper.toOrderBookResponse(orderBookData);
            eventPublisher.publishEvent(new OrderBookUpdatedEvent(response));
        } catch (Exception e) {
            log.error("Error processing order book update: {}", event.orderBookData(), e);
            // Thêm retry logic nếu cần
            retryHandleOrderBookReceived(event);
        }
    }

    private void retryHandleOrderBookReceived(OrderBookReceivedEvent event) {
        try {
            Thread.sleep(1000); // Delay trước khi retry
            OrderBookData orderBookData = event.orderBookData();
            OrderBookResponse response = marketDataMapper.toOrderBookResponse(orderBookData);
            eventPublisher.publishEvent(new OrderBookUpdatedEvent(response));
        } catch (Exception e) {
            log.error("Retry failed for order book update: {}", event.orderBookData(), e);
        }
    }
}