package com.ntd.exchange_crypto.market;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderBookEntry {
    private BigDecimal priceLevel;
    private BigDecimal quantity;
}
