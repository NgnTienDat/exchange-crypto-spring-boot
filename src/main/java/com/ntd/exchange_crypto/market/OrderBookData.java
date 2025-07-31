package com.ntd.exchange_crypto.market;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderBookData {
    private String productId;
    private List<OrderBookEntry> bids;
    private List<OrderBookEntry> asks;
    private Long lastUpdateId;
}

