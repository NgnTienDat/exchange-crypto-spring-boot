package com.ntd.exchange_crypto.trade.service;

import com.ntd.exchange_crypto.trade.model.Trade;
import com.ntd.exchange_crypto.trade.repository.TradeRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TradeService {

    TradeRepository tradeRepository;

    public void test() {
        System.out.println("🔥 TradeService is running");
    }

    public void saveTrade(Trade trade) {
        tradeRepository.save(trade);
    }
}
