package com.ntd.exchange_crypto.asset.service;

import com.ntd.exchange_crypto.asset.AssetExternalAPI;
import com.ntd.exchange_crypto.asset.AssetInternalAPI;
import com.ntd.exchange_crypto.asset.dto.response.AssetResponse;
import com.ntd.exchange_crypto.asset.mapper.AssetMapper;
import com.ntd.exchange_crypto.asset.model.Asset;
import com.ntd.exchange_crypto.asset.repository.AssetRepository;
import com.ntd.exchange_crypto.cryptocurrency.CryptoExternalAPI;
import com.ntd.exchange_crypto.asset.exception.AssetErrorCode;
import com.ntd.exchange_crypto.asset.exception.AssetException;
import com.ntd.exchange_crypto.cryptocurrency.model.Crypto;
import com.ntd.exchange_crypto.user.UserDTO;
import com.ntd.exchange_crypto.user.UserExternalAPI;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;


@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AssetService implements AssetInternalAPI, AssetExternalAPI {

    AssetRepository assetRepository;
    UserExternalAPI userExternalAPI;


    /* --------------------------------------- External --------------------------------------- */


    @Override
    public BigDecimal getAvailableBalance(String productId) {


        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        UserDTO user = userExternalAPI.userExistsByEmail(email);
        Asset asset = this.assetRepository.findAssetsByUserIdAndCryptoId(user.getId(), productId);
        if (asset == null) {
            throw new AssetException(AssetErrorCode.ASSET_NOTFOUND);
        }
        return asset.getBalance().subtract(asset.getLockedBalance());
    }

    @Override
    public Optional<Asset> lockBalance(String userId, String productId, BigDecimal amount) {
        return Optional.empty();
    }

    @Override
    public Optional<Asset> unlockBalance(String userId, String productId, BigDecimal amount) {
        return Optional.empty();
    }


    /* --------------------------------------- Internal --------------------------------------- */


//    @Override
//    public AssetResponse update(String productId, BigDecimal newBalance) {
//        var context = SecurityContextHolder.getContext();
//        String email = context.getAuthentication().getName();
//        UserDTO user = userExternalAPI.userExistsByEmail(email);
//
//
//        boolean existCrypto = this.cryptoExternalAPI.existsById(productId);
//
//
//
//        Asset asset = this.assetRepository.findAssetsByUserIdAndCryptoId(user.getId(), productId);
//        if (asset == null) {
//            Asset newAsset = Asset.builder()
//                    .balance(newBalance)
//                    .lockedBalance(new BigDecimal("0"))
//                    .cryptoId(productId)
//                    .lastUpdated(Instant.now())
//                    .status(Asset.AssetStatus.ACTIVE)
//                    .userId(user.getId())
//                    .build();
//            asset = this.assetRepository.save(newAsset);
//
//        }
//
//        return assetMapper.toAssetResponse(asset);
//    }

    @Override
    public Optional<Asset> freezeAsset(String userId, String productId) {
        return Optional.empty();
    }

    @Override
    public Optional<Asset> unfreezeAsset(String userId, String productId) {
        return Optional.empty();
    }
}
