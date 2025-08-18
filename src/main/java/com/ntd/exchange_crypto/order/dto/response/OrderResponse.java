package com.ntd.exchange_crypto.order.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class OrderResponse {
    String id;
    String pairId;
    String side;
    String type;
    String status;
    BigDecimal price;
    BigDecimal quantity;
    BigDecimal filledQuantity;
    BigDecimal remainingQuantity;
    String createdAt;
    String updatedAt;

}
