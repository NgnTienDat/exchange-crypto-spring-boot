package com.ntd.exchange_crypto.cryptocurrency.repository;

import com.ntd.exchange_crypto.cryptocurrency.model.Crypto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CryptoRepository extends JpaRepository<Crypto, String> {
}
