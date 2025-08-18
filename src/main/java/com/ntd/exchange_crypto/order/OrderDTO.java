package com.ntd.exchange_crypto.order;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDTO {
    String id;
    String userId;
    String pairId;
    String side;
    String type;
    BigDecimal quantity;
    BigDecimal price;
    String status;
    BigDecimal filledQuantity;
    BigDecimal remainingQuantity;
    String createdAt;
    String updatedAt;
}


