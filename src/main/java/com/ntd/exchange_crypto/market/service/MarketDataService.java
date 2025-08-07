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
import com.ntd.exchange_crypto.market.dto.response.CandleStickResponse;
import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;
import com.ntd.exchange_crypto.market.dto.response.MarketTradeResponse;
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

            OrderBookResponse response = marketDataMapper.toOrderBookResponse(orderBookData);
            eventPublisher.publishEvent(new OrderBookUpdatedEvent(response));
        } catch (Exception e) {
            log.error("Error processing order book update: {}", event.orderBookData(), e);
//            retryHandleOrderBookReceived(event);
        }
    }

    @EventListener
    @Async("websocketExecutor")
    public void handleCandleStickReceived(CandleStickReceivedEvent event) {
        try {
            CandleStick candleStick = event.candleStick();

            CandleStickResponse response = marketDataMapper.toCandleStickResponse(candleStick);
            eventPublisher.publishEvent(new CandleStickUpdatedEvent(response));
        } catch (Exception e) {
            log.error("Error processing order book update: {}", event.candleStick(), e);
        }
    }


    @EventListener
    @Async("websocketExecutor")
    public void handleMarketTradeReceived(MarketTradeReceivedEvent event) {
        try {
            MarketTrade marketTrade = event.marketTrade();



            MarketTradeResponse response = marketDataMapper.toMarketTradeResponse(marketTrade);
            eventPublisher.publishEvent(new MarketTradeUpdatedEvent(response));
        } catch (Exception e) {
            log.error("Error processing order book update: {}", event.marketTrade(), e);
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