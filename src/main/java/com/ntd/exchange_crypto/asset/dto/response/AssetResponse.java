package com.ntd.exchange_crypto.asset.dto.response;

import com.ntd.exchange_crypto.asset.model.Asset;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class AssetResponse {
    String id;
    String cryptoId;
    String userId;
    BigDecimal balance;
    BigDecimal lockedBalance;
    Instant lastUpdated;
    String assetStatus;
}
