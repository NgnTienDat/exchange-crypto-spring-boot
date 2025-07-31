package com.ntd.exchange_crypto.market.mapper;

import com.ntd.exchange_crypto.market.CandleStick;
import com.ntd.exchange_crypto.market.OrderBookData;
import com.ntd.exchange_crypto.market.OrderBookEntry;
import com.ntd.exchange_crypto.market.dto.response.CandleStickResponse;
import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;
import com.ntd.exchange_crypto.market.MarketData;
import com.ntd.exchange_crypto.market.dto.response.OrderBookResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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

        // Chuyển đổi danh sách bids từ OrderBookEntry sang OrderBookEntryResponse
        List<OrderBookEntry> bids = orderBookData.getBids().stream()
                .map(entry -> OrderBookEntry.builder()
                        .priceLevel(entry.getPriceLevel())
                        .quantity(entry.getQuantity())
                        .build())
                .toList();

        // Chuyển đổi danh sách asks từ OrderBookEntry sang OrderBookEntryResponse
        List<OrderBookEntry> asks = orderBookData.getAsks().stream()
                .map(entry -> OrderBookEntry.builder()
                        .priceLevel(entry.getPriceLevel())
                        .quantity(entry.getQuantity())
                        .build())
                .toList();

        return OrderBookResponse.builder()
                .productId(orderBookData.getProductId())
                .bids(bids)
                .asks(asks)
                .lastUpdateId(orderBookData.getLastUpdateId())
                .build();
    }

    @Override
    public CandleStickResponse toCandleStickResponse(CandleStick candleStick) {
        if (candleStick == null) {
            return null;
        }

        return CandleStickResponse.builder()
                .productId(candleStick.getProductId())
                .timestamp(candleStick.getTimestamp())
                .open(candleStick.getOpen())
                .high(candleStick.getHigh())
                .low(candleStick.getLow())
                .close(candleStick.getClose())
                .volume(candleStick.getVolume())
                .totalVolume(candleStick.getTotalVolume())
                .isFinal(candleStick.isFinal())
                .build();
    }
}

