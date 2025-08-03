package com.ntd.exchange_crypto.asset.repository;

import com.ntd.exchange_crypto.asset.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, String> {
    Asset findAssetsByUserIdAndCryptoId(String userId, String cryptoId);
}
