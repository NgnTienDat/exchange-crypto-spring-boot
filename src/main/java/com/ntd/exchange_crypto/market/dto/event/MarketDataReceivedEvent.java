package com.ntd.exchange_crypto.market.dto.event;

import com.ntd.exchange_crypto.market.model.MarketData;

public record MarketDataReceivedEvent(MarketData marketData) {}