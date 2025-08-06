package com.ntd.exchange_crypto.asset;


import com.ntd.exchange_crypto.asset.dto.response.AssetResponse;
import com.ntd.exchange_crypto.asset.model.Asset;

import java.math.BigDecimal;
import java.util.Optional;

public interface AssetInternalAPI {
    AssetResponse createNewAsset(String productId, BigDecimal newBalance);

    Optional<Asset> freezeAsset(String userId, String productId);

    Optional<Asset> unfreezeAsset(String userId, String productId);
}
