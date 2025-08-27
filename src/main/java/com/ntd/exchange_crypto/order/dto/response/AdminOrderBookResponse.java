package com.ntd.exchange_crypto.order.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class AdminOrderBookResponse {
    List<OrderResponse> ordersBid;
    List<OrderResponse> ordersAsk;
}
