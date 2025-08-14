package com.ntd.exchange_crypto.order;


import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDTO {
    String userId;
    String pairId;
    String side;
    String type;
    String quantity;
    String price;
}
