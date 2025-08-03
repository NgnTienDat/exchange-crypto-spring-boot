package com.ntd.exchange_crypto.cryptocurrency.dto.request;


import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class CryptoCreationRequest {
    String productId;
    String name;
    String symbol;

}
