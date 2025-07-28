package com.ntd.exchange_crypto.market.mapper;

import com.ntd.exchange_crypto.market.dto.response.MarketTickerResponse;
import com.ntd.exchange_crypto.market.MarketData;
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

        return new MarketTickerResponse(
                productId,
                price,
                volume24h,
                low24h,
                high24h,
                low52w,
                high52w,
                priceChangePercent24h,
                trend,
                timestamp
        );
    }
}

