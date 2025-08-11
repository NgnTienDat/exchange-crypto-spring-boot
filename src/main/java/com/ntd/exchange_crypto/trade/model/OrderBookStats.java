package com.ntd.exchange_crypto.trade.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderBookStats {
    BigDecimal minAskPrice;
    BigDecimal maxAskPrice;
    BigDecimal minBidPrice;
    BigDecimal maxBidPrice;
}
