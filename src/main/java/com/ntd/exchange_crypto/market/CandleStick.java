package com.ntd.exchange_crypto.market;

import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CandleStick {
    private String productId;
    private long timestamp;       // Thời gian bắt đầu của nến (epoch millis)
    private double open;          // Giá mở cửa
    private double high;          // Giá cao nhất
    private double low;           // Giá thấp nhất
    private double close;         // Giá đóng cửa
    private double volume;        // Khối lượng giao dịch
    private double totalVolume;        // Tổng giá trị giao dịch
    private boolean isFinal;
}
