package com.ntd.exchange_crypto.asset.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "asset")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, length = 36)
    String id;

    String cryptoId;

    @Column(name = "user_id", nullable = false, length = 36)
    String userId;

    @Column(name = "balance", nullable = false, precision = 18, scale = 8)
    BigDecimal balance;

    @Column(name = "locked_balance", nullable = false, precision = 18, scale = 8)
    BigDecimal lockedBalance;

    @Column(name = "last_updated", nullable = false)
    Instant lastUpdated;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    AssetStatus status;

    public enum AssetStatus {
        ACTIVE, FROZEN
    }

}
