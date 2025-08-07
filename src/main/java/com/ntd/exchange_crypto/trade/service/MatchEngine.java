package com.ntd.exchange_crypto.trade.service;

import com.ntd.exchange_crypto.market.OrderBookData;
import com.ntd.exchange_crypto.order.OrderExternalAPI;
import com.ntd.exchange_crypto.order.model.Order;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class MatchEngine {
    OrderBookStatsService orderBookStatsService;
    OrderExternalAPI orderExternalAPI;

    // Hàm lắng nghe order mới từ Redis pub/sub
    public void processNewOrder(Order order) {
        if(order==null || order.getId() == null) {
            log.error("Received null or invalid order");
            return;
        }
        // 2. Phân loại lệnh: MARKET hoặc LIMIT
        // 3. Gọi xử lý phù hợp: handleMarketOrder hoặc handleLimitOrder
    }

    // Xử lý lệnh Market
    private void handleMarketOrder(Order order) {
        // 1. Xác định chiều lệnh (BID hoặc ASK)
        // 2. Lấy bestAsk hoặc bestBid từ cache OrderBookData
        // 3. So sánh order với best giá (nếu có order tốt hơn => khớp lệnh)
        //    => tạo giao dịch thực với user tương ứng
        // 4. Nếu không có order tốt hơn: khớp với anonymous user theo best price
        // 5. Gửi event tạo giao dịch / lưu vào DB
    }

    // Xử lý lệnh Limit
    private void handleLimitOrder(Order order) {
        // 1. Xác định chiều lệnh (BID hoặc ASK)
        // 2. Kiểm tra có order đối ứng cùng giá trong Redis OrderBook
        //    - Nếu có: tạo giao dịch thực (khớp toàn phần / một phần)
        //    - Nếu không:
        //        + Nếu giá nằm trong min-max đối ứng => delay random 5-30s rồi khớp với anonymous user
        //        + Nếu giá nằm ngoài min-max => đặt trạng thái PENDING
        // 3. Gửi event tạo giao dịch hoặc cập nhật trạng thái lệnh
    }

    // Cập nhật dữ liệu OrderBook từ Binance (OrderBookData)
    public void updateOrderBookData(OrderBookData data) {
        // 1. Cập nhật bestBid và bestAsk vào cache
        // 2. Cập nhật min/max của BID và ASK
        // 3. Nếu có các lệnh LIMIT đang PENDING và giá hiện tại đã vào khoảng min-max
        //    => xét và tạo giao dịch với anonymous user nếu phù hợp
    }

    // Hàm khớp lệnh (thực hiện giao dịch)
    private void match(Order takerOrder, Order makerOrder, BigDecimal matchPrice, BigDecimal matchQuantity) {
        // 1. Tạo bản ghi Transaction (Giao dịch)
        // 2. Cập nhật lại Order của cả hai bên (giảm quantity, status...)
        // 3. Cập nhật lại OrderBook Redis nếu cần
        // 4. Gửi event để các module khác nhận biết
    }

    // Hàm khớp với anonymous user
    private void matchWithAnonymous(Order order, BigDecimal matchPrice, BigDecimal matchQuantity) {
        // 1. Tạo Transaction với user ảo
        // 2. Đánh dấu order đã khớp
        // 3. Gửi event khớp lệnh
    }

    // Hàm delay khớp với anonymous sau 5-30s
    private void scheduleAnonymousMatch(Order order, Duration delay) {
        // 1. Sử dụng ScheduledExecutorService hoặc TaskScheduler để delay
        // 2. Sau delay, kiểm tra lại khoảng giá và khớp nếu hợp lệ
    }

    // Hàm kiểm tra order limit PENDING có nên khớp với anonymous không
    private void checkPendingOrdersAgainstOrderBook() {
        // 1. Lấy danh sách các order LIMIT có trạng thái PENDING
        // 2. So sánh khoảng giá hiện tại với price của từng order
        // 3. Nếu vào vùng min-max => khớp với anonymous user
    }

}
