package com.ntd.exchange_crypto.market;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MarketTrade {
    private String productId;       // Cặp tiền (ví dụ: "BTCUSDT")
    private Long tradeId;           // Số thứ tự giao dịch duy nhất
    private BigDecimal price;       // Giá giao dịch
    private BigDecimal quantity;    // Khối lượng giao dịch
    private Long tradeTime;         // Thời gian giao dịch (epoch milliseconds)
    private boolean isMaker;        // True nếu là maker, false nếu là taker
    private BigDecimal totalValue;  // Tổng giá trị giao dịch (price * quantity)
}