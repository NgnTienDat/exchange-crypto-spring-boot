package com.ntd.exchange_crypto.cryptocurrency.service;

import com.ntd.exchange_crypto.cryptocurrency.CryptoExternalAPI;
import com.ntd.exchange_crypto.cryptocurrency.CryptoInternalAPI;
import com.ntd.exchange_crypto.cryptocurrency.exception.CryptoErrorCode;
import com.ntd.exchange_crypto.cryptocurrency.exception.CryptoException;
import com.ntd.exchange_crypto.cryptocurrency.model.Crypto;
import com.ntd.exchange_crypto.cryptocurrency.repository.CryptoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CryptoService implements CryptoInternalAPI, CryptoExternalAPI {

    private final CryptoRepository cryptoRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Crypto> getCryptoById(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new CryptoException(CryptoErrorCode.INVALID_CRYPTO);
        }
        return cryptoRepository.findById(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new CryptoException(CryptoErrorCode.CRYPTO_DOES_NOT_EXIST);
        }
        return cryptoRepository.existsById(productId);
    }

//    @Override
//    public Optional<Crypto> updateCryptoData(String productId, BigDecimal totalSupply, BigDecimal price) {
//        if (productId == null || productId.trim().isEmpty()) {
//            throw new IllegalArgumentException("Product ID cannot be null or empty");
//        }
//        if (totalSupply == null || price == null) {
//            throw new IllegalArgumentException("Total supply and price cannot be null");
//        }
//
//        return cryptoRepository.findById(productId)
//                .map(crypto -> {
//                    crypto.setTotalSupply(totalSupply);
//                    crypto.setLastUpdated(Instant.now());
//                    // Cập nhật giá nếu cần (tùy chọn, có thể thêm trường price vào Crypto model)
//                    return cryptoRepository.save(crypto);
//                });
//    }

//    @Override
//    public Crypto createCrypto(CryptoCreationRequest cryptoCreationRequest) {
//
//        if (cryptoRepository.existsById(productId)) {
//            throw new IllegalStateException("Crypto with productId " + productId + " already exists");
//        }
//
//        Crypto crypto = Crypto.builder()
//                .productId(productId)
//                .name(name)
//                .symbol(symbol)
//                .lastUpdated(Instant.now())
//                .build();
//        return cryptoRepository.save(crypto);
//    }

    @Override
    public boolean deleteCrypto(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }

        return cryptoRepository.findById(productId)
                .map(crypto -> {
                    cryptoRepository.delete(crypto);
                    return true;
                }).orElse(false);
    }
}