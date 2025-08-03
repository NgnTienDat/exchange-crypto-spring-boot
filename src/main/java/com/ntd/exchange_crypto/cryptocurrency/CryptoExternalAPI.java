package com.ntd.exchange_crypto.cryptocurrency;


import com.ntd.exchange_crypto.cryptocurrency.model.Crypto;

import java.util.Optional;

public interface CryptoExternalAPI {

    Optional<Crypto> getCryptoById(String productId);

    boolean existsById(String productId);
}
