package com.ntd.exchange_crypto.auth.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "trusted_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrustedDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 255)
    private String deviceId; // UUID sinh từ client hoặc hash fingerprint

    @Column(length = 500)
    private String userAgent; // thông tin trình duyệt, hệ điều hành

    @Column(length = 100)
    private String ipAddress; // IP đăng nhập

    @Column(nullable = false)
    private boolean verified = false; // true = đã xác thực 2FA

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant verifiedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }


}
