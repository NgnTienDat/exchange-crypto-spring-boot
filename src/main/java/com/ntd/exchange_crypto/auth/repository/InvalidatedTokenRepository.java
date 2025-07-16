package com.ntd.exchange_crypto.auth.repository;


import com.ntd.exchange_crypto.auth.model.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {
}
