package com.ntd.exchange_crypto.trade;

import com.ntd.exchange_crypto.market.OrderBookData;
import com.ntd.exchange_crypto.market.OrderBookEntry;
import com.ntd.exchange_crypto.trade.model.OrderBookStats;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderBookStatsService {
    private final Map<String, OrderBookStats> statsCache = new ConcurrentHashMap<>();

    public void updateStats(String productId, OrderBookData orderBook) {
        // Lấy danh sách ask/bid từ OrderBookData

        List<OrderBookEntry> asks = orderBook.getAsks();
        List<OrderBookEntry> bids = orderBook.getBids();

        if (asks.isEmpty() || bids.isEmpty()) return;

        // sort theo price
        BigDecimal minAsk = asks.getFirst().getPriceLevel();
        BigDecimal maxAsk = asks.getLast().getPriceLevel();
        BigDecimal minBid = bids.getLast().getPriceLevel(); // bid reverse sort
        BigDecimal maxBid = bids.getFirst().getPriceLevel();

        BigDecimal rangeAsk = maxAsk.subtract(minAsk);
        BigDecimal rangeBid = maxBid.subtract(minBid);

        OrderBookStats stats = new OrderBookStats(minAsk, maxBid, rangeAsk, rangeBid);
        statsCache.put(productId, stats);
//        System.out.printf("------------------------\nUpdated stats for %s\n: " +
//                        "Min Ask: %s\n, Max Ask: %s\n, Min Bid: %s\n, Max Bid: %s%n\n",
//                productId, minAsk, maxAsk, minBid, maxBid);
    }

    public OrderBookStats getStats(String productId) {
        return statsCache.get(productId);
    }
}
