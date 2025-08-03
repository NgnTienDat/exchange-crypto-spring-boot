package com.ntd.exchange_crypto.cryptocurrency.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Entity
@Table(name = "crypto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Crypto {

    @Id
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "symbol", nullable = false, length = 10)
    private String symbol;

//    @Column(name = "total_supply", precision = 18, scale = 8)
//    private BigDecimal totalSupply; // Tổng cung tối đa
//
//    @Column(name = "launch_date")
//    private Instant launchDate; // Ngày ra mắt
//
//    @Column(name = "description", length = 500)
//    private String description; // Mô tả ngắn
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "blockchain", length = 20)
//    private Blockchain blockchain; // Blockchain (ví dụ: BTC, ETH, TRON)
//
//    // Enum cho blockchain
//    public enum Blockchain {
//        BITCOIN, ETHEREUM, TRON, BINANCE_SMART_CHAIN
//    }
}