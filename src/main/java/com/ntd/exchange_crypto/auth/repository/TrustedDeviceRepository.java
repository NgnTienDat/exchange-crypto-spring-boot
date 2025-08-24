package com.ntd.exchange_crypto.auth.repository;

import com.ntd.exchange_crypto.auth.model.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, String> {
    Optional<TrustedDevice> findByDeviceIdAndUserId(String deviceId, String userId);
}
