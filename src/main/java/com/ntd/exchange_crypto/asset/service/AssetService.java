package com.ntd.exchange_crypto.asset.service;

import com.ntd.exchange_crypto.asset.AssetExternalAPI;
import com.ntd.exchange_crypto.asset.AssetInternalAPI;
import com.ntd.exchange_crypto.asset.dto.response.AssetResponse;
import com.ntd.exchange_crypto.asset.mapper.AssetMapper;
import com.ntd.exchange_crypto.asset.model.Asset;
import com.ntd.exchange_crypto.asset.repository.AssetRepository;
import com.ntd.exchange_crypto.asset.exception.AssetErrorCode;
import com.ntd.exchange_crypto.asset.exception.AssetException;
import com.ntd.exchange_crypto.user.UserDTO;
import com.ntd.exchange_crypto.user.UserExternalAPI;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AssetService implements AssetInternalAPI, AssetExternalAPI {

    AssetRepository assetRepository;
    UserExternalAPI userExternalAPI;
    AssetMapper assetMapper;


    /* --------------------------------------- External --------------------------------------- */


    @Override
    public BigDecimal getAvailableBalance(String cryptoId) {
        UserDTO user = userExternalAPI.getUserLogin();

        Asset asset = this.assetRepository
                .findAssetsByUserIdAndCryptoId(user.getId(), cryptoId)
                .orElseThrow(() -> new AssetException(AssetErrorCode.USER_ASSET_NOTFOUND));

        return asset.getBalance().subtract(asset.getLockedBalance());
    }

    @Override
    public List<AssetResponse> getMyAsset() {
        UserDTO user = userExternalAPI.getUserLogin();
        Set<Asset> assets = this.assetRepository.findAllByUserId(user.getId());
        if (assets.isEmpty()) {
            log.warn("No assets found for user: {}", user.getId());
            return List.of();
        }
        return assets.stream()
                .map(assetMapper::toAssetResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasSufficientBalance(String cryptoId, BigDecimal amount) {
        BigDecimal availableBalance = getAvailableBalance(cryptoId);
        return availableBalance.compareTo(amount) >= 0;
    }

    /*
    *
    *
    * */

    @Override
    @Transactional
    public void lockBalance(String giveCryptoId, BigDecimal amount) {

        UserDTO userDTO = userExternalAPI.getUserLogin();

        log.info("Locking balance for user: {}, cryptoId: {}, amount: {}",
                userDTO.getId(), giveCryptoId, amount);

        Asset userAsset = this.assetRepository
                .findAssetsByUserIdAndCryptoId(userDTO.getId(), giveCryptoId)
                .orElseThrow(() -> new AssetException(AssetErrorCode.USER_ASSET_NOTFOUND));


        if (getAvailableBalance(giveCryptoId).compareTo(amount) < 0)
            throw new AssetException(AssetErrorCode.INSUFFICIENT_BALANCE_TO_LOCK);

        userAsset.setLockedBalance(userAsset.getLockedBalance().add(amount));
        assetRepository.save(userAsset);
    }

    @Override
    @Transactional
    public void unlockBalance(String userId, String giveCryptoId, BigDecimal amount) {
        log.info("Unlocking balance for user");

        Asset userAsset = this.assetRepository
                .findAssetsByUserIdAndCryptoId(userId, giveCryptoId)
                .orElseThrow(() -> new AssetException(AssetErrorCode.USER_ASSET_NOTFOUND));

        log.info("UNLOCK AMOUNT: {}, LOCKED BALANCE: {}", amount, userAsset.getLockedBalance());
        if (userAsset.getLockedBalance().compareTo(amount) < 0)
            throw new AssetException(AssetErrorCode.LOCKED_BALANCE_INSUFFICIENT);

        userAsset.setLockedBalance(userAsset.getLockedBalance().subtract(amount));
        assetRepository.saveAndFlush(userAsset);
    }

    @Override
    public void updateAsset(String userId, String cryptoId, BigDecimal amount, String side) {
        Asset userAsset = assetRepository.findAssetsByUserIdAndCryptoId(userId, cryptoId)
                .orElse(null);

        if (userAsset == null) {
            if ("BID".equals(side)) {
                userAsset = Asset.builder()
                        .balance(amount)
                        .lockedBalance(BigDecimal.ZERO)
                        .cryptoId(cryptoId)
                        .lastUpdated(Instant.now())
                        .status(Asset.AssetStatus.ACTIVE)
                        .userId(userId)
                        .build();
            } else {
                throw new AssetException(AssetErrorCode.USER_ASSET_NOTFOUND);
            }
        } else {
            userAsset.setBalance(userAsset.getBalance().add(amount));
            userAsset.setLastUpdated(Instant.now());
        }

        log.info("Updating asset for user: {}, cryptoId: {}, amount: {}", userId, cryptoId, amount);
        assetRepository.save(userAsset);
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
    public AssetResponse createNewAsset(String productId, BigDecimal newBalance) {

        UserDTO user = userExternalAPI.getUserLogin();

        Asset asset = this.assetRepository
                .findAssetsByUserIdAndCryptoId(user.getId(), productId)
                .orElseGet(() -> {
                    Asset newAsset = Asset.builder()
                            .balance(newBalance)
                            .lockedBalance(BigDecimal.ZERO)
                            .cryptoId(productId)
                            .lastUpdated(Instant.now())
                            .status(Asset.AssetStatus.ACTIVE)
                            .userId(user.getId())
                            .build();
                    return this.assetRepository.save(newAsset);
                });
        return assetMapper.toAssetResponse(asset);

    }

    @Override
    public Optional<Asset> freezeAsset(String userId, String productId) {
        return Optional.empty();
    }

    @Override
    public Optional<Asset> unfreezeAsset(String userId, String productId) {
        return Optional.empty();
    }
}
