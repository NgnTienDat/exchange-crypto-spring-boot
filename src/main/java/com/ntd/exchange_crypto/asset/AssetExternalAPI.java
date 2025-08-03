package com.ntd.exchange_crypto.asset;


import com.ntd.exchange_crypto.asset.model.Asset;

import java.math.BigDecimal;
import java.util.Optional;

public interface AssetExternalAPI {
    BigDecimal getAvailableBalance(String productId);

    Optional<Asset> lockBalance(String userId, String productId, BigDecimal amount);

    Optional<Asset> unlockBalance(String userId, String productId, BigDecimal amount);
}
