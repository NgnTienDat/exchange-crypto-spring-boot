package com.ntd.exchange_crypto.trade.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntd.exchange_crypto.market.OrderBookData;
import com.ntd.exchange_crypto.order.OrderExternalAPI;
import com.ntd.exchange_crypto.order.enums.OrderStatus;
import com.ntd.exchange_crypto.order.enums.Side;
import com.ntd.exchange_crypto.order.model.Order;
import com.ntd.exchange_crypto.trade.model.OrderBookStats;
import com.ntd.exchange_crypto.trade.model.Trade;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;

@Slf4j
@Component
public class MatchEngine {
    private final TradeService tradeService;
    private final OrderBookStatsService orderBookStatsService;
    private final OrderExternalAPI orderExternalAPI;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public MatchEngine(TradeService tradeService, OrderBookStatsService orderBookStatsService, OrderExternalAPI orderExternalAPI, RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.tradeService = tradeService;
        this.orderBookStatsService = orderBookStatsService;
        this.orderExternalAPI = orderExternalAPI;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }


    // Hàm lắng nghe order mới từ Redis pub/sub
    public void processNewOrder(Order order) throws JsonProcessingException {
        if (order == null || order.getId() == null) {
            log.error("Received null or invalid order");
            return;
        }
        switch (order.getType()) {
            case MARKET:
                System.out.println("🔥 Nhận order mới: " + order);
                handleMarketOrder(order);
                break;

            case LIMIT:
                handleLimitOrder(order);
                break;

            default:
                log.warn("Unsupported order type: {}", order.getType());
                break;
        }
    }

    // Xử lý lệnh Market
    private void handleMarketOrder(Order order) throws JsonProcessingException {
        // 1. Xác định chiều lệnh (BID hoặc ASK)
        // 2. Lấy bestAsk hoặc bestBid từ cache orderBookStatsService
        // 3. Lấy order đối ứng có giá tốt nhất từ Redis OrderBook
        // 4. Nếu giá của order đối ứng tốt hơn hoặc bằng best giá hiện tại:
        //    - Khớp lệnh với order đối ứng
        //    - Tạo giao dịch thực với user tương ứng
        // 5. Nếu không có order đối ứng tốt hơn:
        //    - Khớp với anonymous user theo best giá hiện tại
        //    - Tạo giao dịch với user ảo
        // 6. Gửi event tạo giao dịch / lưu vào DB

        System.out.println("🔥 Nhận order mới market: " + order);


        // 1. Xác định chiều lệnh (BID hoặc ASK)
        Side side = order.getSide();
        String productId = this.getPairIdFromOrderBookData(side, order.getGiveCryptoId(), order.getGetCryptoId());

        // 2. Lấy stats từ cache (đã cập nhật liên tục bởi BinanceWebSocketService)
        OrderBookStats stats = orderBookStatsService.getStats(productId);
        if (stats == null) {
            log.warn("No order book stats available for {}", productId);
            return;
        }

        BigDecimal bestPrice = (side == Side.BID) ? stats.getMinAskPrice() : stats.getMaxBidPrice();
        System.out.println("🔥 Best price for " + productId + ": " + bestPrice);

        String pairId = orderExternalAPI.getPairId(side, order.getGiveCryptoId(), order.getGetCryptoId());

        // 3. Tìm order đối ứng từ Redis (RedisZSet theo chiều ngược lại)
        Side counterSide = (side == Side.BID) ? Side.ASK : Side.BID;
        String redisZSetKey = "orderbook:" + pairId + ":" + counterSide.name().toLowerCase();
        System.out.println("🔥 Redis ZSet key: " + redisZSetKey);

        // Lấy order đối ứng có giá tốt nhất từ Redis
        Set<Object> orderRedis = redisTemplate.opsForZSet().range(redisZSetKey, 0, 0);
        System.out.println("🔥 Order Redis: " + orderRedis);


        if (orderRedis != null && !orderRedis.isEmpty()) {
            System.out.println("🔥 Found " + orderRedis.size() + " order stats");

            String counterOrderKey = (String) orderRedis.iterator().next();

            System.out.println("🔥 Found counter order key: " + counterOrderKey);

//            // 4. Lấy order từ Redis Hash
            String orderJson = (String) redisTemplate.opsForHash().get("order:" + counterOrderKey, "order");
            if (orderJson == null) return;
//
            Order counterOrder = objectMapper.readValue(orderJson, Order.class);
            System.out.println("🔥 Counter order: " + counterOrder);
//
//            // 5. So sánh giá
            if ((side == Side.BID && counterOrder.getPrice().compareTo(bestPrice) <= 0) ||
                    (side == Side.ASK && counterOrder.getPrice().compareTo(bestPrice) >= 0)) {

                // ✅ 6. Khớp lệnh giữa 2 user
                match(order, counterOrder);
            } else {
                // ❗7. Không tìm được order đối ứng hợp lệ => tạo deal với anonymous user
                matchWithAnonymous(order, bestPrice, order.getQuantity());
            }
        } else {
            log.warn("No order redis available for {}", redisZSetKey);
            // ❗8. Không có order nào phía đối ứng
//            matchWithAnonymous(order, bestPrice, order.getQuantity());
        }

        // 9. Gửi event lưu giao dịch vào DB hoặc xử lý hậu khớp
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
//    private void match(Order takerOrder, Order makerOrder, BigDecimal matchPrice, BigDecimal matchQuantity) {
    private void match(Order takerOrder, Order makerOrder) {
        // takerOrder: new order vừa nhận
        // makerOrder: counter order đã tìm thấy từ Redis
        // Nếu makerOrder có side là BID thì isBuyerMaker = true


        BigDecimal matchQuantity = takerOrder.getQuantity().min(makerOrder.getQuantity());
        boolean isBuyerMaker = makerOrder.getSide() == Side.BID;

        // 1. Tạo bản ghi Transaction (Giao dịch)
        Trade trade = Trade.builder()
                .takerOrderId(takerOrder.getId())
                .makerOrderId(makerOrder.getId())
                .productId(takerOrder.getGiveCryptoId() + "-" + takerOrder.getGetCryptoId())
                .price(makerOrder.getPrice()) // Giá khớp
                .quantity(matchQuantity) // Số lượng khớp
                .isBuyerMaker(isBuyerMaker)
                .build();
        // Lưu giao dịch vào DB (hoặc gửi event để lưu sau)?
        tradeService.saveTrade(trade);


        // 2. Cập nhật lại Order của cả hai bên (giảm quantity, status...)
        //   - Nếu quantity bằng nhau thì cả hai đều là FILLED
        //   - Nếu order nào có quantity nhỏ hơn thì cập nhật status là FILLED
        //   - Ngược lại order lớn hơn còn lại sẽ là PARTIALLY_FILLED
        //   - Cập nhật lại quantity đã khớp (quantityFilled) cho cả hai order
        //   - Cập nhật lại trạng thái của cả hai order
        if (takerOrder.getQuantity().compareTo(makerOrder.getQuantity()) == 0) {
            takerOrder.setStatus(OrderStatus.FILLED);
            makerOrder.setStatus(OrderStatus.FILLED);

        } else if (takerOrder.getQuantity().compareTo(makerOrder.getQuantity()) < 0) {
            // Taker order nhỏ hơn => taker là FILLED, maker là PARTIALLY_FILLED
            takerOrder.setStatus(OrderStatus.FILLED);
            makerOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
        } else {
            // Maker order nhỏ hơn => maker là FILLED, taker là PARTIALLY_FILLED
            makerOrder.setStatus(OrderStatus.FILLED);
            takerOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
        }
        orderExternalAPI.updateOrderStatus(takerOrder, matchQuantity);
        orderExternalAPI.updateOrderStatus(makerOrder, matchQuantity);
        //

        // 3. Cập nhật lại OrderBook Redis nếu cần
        //   - Nếu order đã khớp hết quantity => xóa khỏi OrderBook (Hash và ZSet)
        //   - Nếu chỉ khớp một phần thì cập nhật lại quantity trong Redis Hash
        try {
            updateOrderInOrderBookRedis(takerOrder);
            updateOrderInOrderBookRedis(makerOrder);
        } catch (JsonProcessingException e) {
            log.error("Error updating order in Redis: {}", e.getMessage());
        }

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


    /*--------------- Hàm tiện ích -------------------------------------------------------------------------------*/
    public void test() {
        System.out.println("🔥 MatchEngine is running");
    }

    public String getPairIdFromOrderBookData(Side side, String giveCryptoId, String getCryptoId) {
        return side == Side.BID ?
                getCryptoId + giveCryptoId :
                giveCryptoId + getCryptoId;
    }

    void updateOrderInOrderBookRedis(Order order) throws JsonProcessingException {
        String hashKey = "order:" + order.getId();

        if (order.getStatus() == OrderStatus.FILLED) {
            String zsetKey = "orderbook:" +
                    orderExternalAPI.getPairId(order.getSide(), order.getGiveCryptoId(), order.getGetCryptoId()) +
                    ":" + order.getSide().name().toLowerCase();

            redisTemplate.delete(hashKey);
            redisTemplate.opsForZSet().remove(zsetKey, order.getId());
        } else {
            redisTemplate.opsForHash().put(hashKey, "order", objectMapper.writeValueAsString(order));
        }

    }
}
