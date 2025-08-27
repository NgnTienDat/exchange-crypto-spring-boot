package com.ntd.exchange_crypto.trade.service;

import com.ntd.exchange_crypto.common.SliceResponse;
import com.ntd.exchange_crypto.order.exception.OrderErrorCode;
import com.ntd.exchange_crypto.order.exception.OrderException;
import com.ntd.exchange_crypto.trade.TradeExternalApi;
import com.ntd.exchange_crypto.trade.dto.response.TradeResponse;
import com.ntd.exchange_crypto.trade.exception.TradeErrorCode;
import com.ntd.exchange_crypto.trade.exception.TradeException;
import com.ntd.exchange_crypto.trade.mapper.TradeMapper;
import com.ntd.exchange_crypto.trade.model.Trade;
import com.ntd.exchange_crypto.trade.repository.TradeRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TradeService implements TradeExternalApi {
    TradeRepository tradeRepository;
    TradeMapper tradeMapper;

    @Override
    public SliceResponse<TradeResponse> getAllTradesAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Slice<Trade> trades = tradeRepository.findAll(pageable);

        if (trades == null || !trades.hasContent()) throw new TradeException(TradeErrorCode.TRADE_NOT_FOUND);

        return new SliceResponse<>(
                trades.getContent().stream()
                        .map(tradeMapper::toTradeResponse)
                        .toList(),
                trades.getNumber(),
                trades.getSize(),
                trades.hasNext()
        );
    }


    @Override
    public void saveTrade(Trade trade) {
        tradeRepository.save(trade);
    }
}
