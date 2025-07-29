package com.ntd.exchange_crypto.market.mapper;

import com.ntd.exchange_crypto.market.OrderBookData;
import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;
import com.ntd.exchange_crypto.market.MarketData;
import com.ntd.exchange_crypto.market.dto.response.OrderBookResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class MarketDataMapperMyImpl implements MarketDataMapper {

    @Override
    public MarketTickerResponse toResponse(MarketData marketData) {
        if (marketData == null) {
            return null;
        }

        String productId = marketData.getProductId();
        BigDecimal price = marketData.getPrice();
        BigDecimal volume24h = marketData.getVolume24h();
        BigDecimal low24h = marketData.getLow24h();
        BigDecimal high24h = marketData.getHigh24h();
        BigDecimal low52w = marketData.getLow52w();
        BigDecimal high52w = marketData.getHigh52w();
        BigDecimal priceChangePercent24h = marketData.getPriceChangePercent24h();
        Instant timestamp = marketData.getTimestamp();

        String trend;
        if (priceChangePercent24h == null || priceChangePercent24h.compareTo(BigDecimal.ZERO) == 0) {
            trend = "STABLE";
        } else if (priceChangePercent24h.compareTo(BigDecimal.ZERO) > 0) {
            trend = "UP";
        } else {
            trend = "DOWN";
        }

        return MarketTickerResponse.builder()
                .productId(productId)
                .price(price)
                .volume24h(volume24h)
                .high24h(high24h)
                .low24h(low24h)
                .low52w(low52w)
                .high52w(high52w)
                .priceChangePercent24h(priceChangePercent24h)
                .trend(trend)
                .timestamp(timestamp)
                .build();
    }

    @Override
    public OrderBookResponse toOrderBookResponse(OrderBookData orderBookData) {
        if (orderBookData == null) {
            return null;
        }

        return OrderBookResponse.builder()
                .productId(orderBookData.getProductId())
                .side(orderBookData.getSide())
                .priceLevel(orderBookData.getPriceLevel())
                .newQuantity(orderBookData.getNewQuantity())
                .timestamp(orderBookData.getTimestamp())
                .build();
    }
}

