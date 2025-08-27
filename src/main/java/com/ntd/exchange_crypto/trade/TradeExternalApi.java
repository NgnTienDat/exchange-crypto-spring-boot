package com.ntd.exchange_crypto.trade;

import com.ntd.exchange_crypto.common.SliceResponse;
import com.ntd.exchange_crypto.trade.dto.response.TradeResponse;
import com.ntd.exchange_crypto.trade.model.Trade;

public interface TradeExternalApi {
    void saveTrade(Trade trade);
    SliceResponse<TradeResponse> getAllTradesAdmin(int page, int size);
}
