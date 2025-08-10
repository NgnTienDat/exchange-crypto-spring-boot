package com.ntd.exchange_crypto.asset;


import com.ntd.exchange_crypto.asset.dto.response.AssetResponse;
import com.ntd.exchange_crypto.asset.model.Asset;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AssetExternalAPI {
    BigDecimal getAvailableBalance(String cryptoId);

    List<AssetResponse> getMyAsset();

    boolean hasSufficientBalance(String productId, BigDecimal amount);

    void lockBalance(String cryptoId, BigDecimal amount);

    void unlockBalance(String userId ,String productId, BigDecimal amount);

    void updateAsset(String userId, String cryptoId, BigDecimal amount);

}
