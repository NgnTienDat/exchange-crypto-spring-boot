package com.ntd.exchange_crypto.asset.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@ToString
public class AssetCreationRequest {
    String productId;
    BigDecimal newBalance;
}
