package com.ntd.exchange_crypto.trade.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntd.exchange_crypto.asset.AssetExternalAPI;
import com.ntd.exchange_crypto.market.OrderBookData;
import com.ntd.exchange_crypto.order.OrderDTO;
import com.ntd.exchange_crypto.order.OrderExternalAPI;
import com.ntd.exchange_crypto.order.OrderReceivedEvent;
import com.ntd.exchange_crypto.order.enums.OrderStatus;
import com.ntd.exchange_crypto.order.enums.OrderType;
import com.ntd.exchange_crypto.order.enums.Side;
import com.ntd.exchange_crypto.order.mapper.OrderMapper;
import com.ntd.exchange_crypto.order.model.Order;
import com.ntd.exchange_crypto.trade.OrderBookStatsService;
import com.ntd.exchange_crypto.trade.model.OrderBookStats;
import com.ntd.exchange_crypto.trade.model.Trade;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MatchEngine {

    private final ApplicationEventPublisher eventPublisher;
    private final TradeService tradeService;
    private final OrderBookStatsService orderBookStatsService;
    private final OrderExternalAPI orderExternalAPI;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final OrderMapper orderMapper;

    public MatchEngine(TradeService tradeService,
                       OrderBookStatsService orderBookStatsService,
                       OrderExternalAPI orderExternalAPI, AssetExternalAPI assetExternalAPI,
                       AssetExternalAPI assetExternalAPI1, SimpMessagingTemplate messagingTemplate,
                       ApplicationEventPublisher eventPublisher, RedisTemplate<String,
                    Object> redisTemplate, ObjectMapper objectMapper, OrderMapper orderMapper) {
        this.tradeService = tradeService;
        this.orderBookStatsService = orderBookStatsService;
        this.orderExternalAPI = orderExternalAPI;
        this.eventPublisher = eventPublisher;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.orderMapper = orderMapper;
    }


    // lắng nghe order mới từ Redis pub/sub
    public void processNewOrder(Order order) throws JsonProcessingException {
        if (order == null || order.getId() == null) {
            log.error("Received null or invalid order");
            return;
        }
        switch (order.getType()) {
            case MARKET:
                log.info("🔥 Nhận order mới: {}", order);
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

        /*
         * ASK bán             BID mua
         * 99                 101
         * 100                100
         * 101                99
         * */


        log.info("🔥 Nhận order mới MARKET: {}", order);


        // 1. Xác định chiều lệnh (BID hoặc ASK)
        Side side = order.getSide();
        String productId = this.getPairIdFromOrderBookData(side, order.getGiveCryptoId(), order.getGetCryptoId());

        // 2. Lấy stats từ cache (đã cập nhật liên tục bởi BinanceWebSocketService)
        OrderBookStats stats = orderBookStatsService.getStats(productId);
        if (stats == null) {
            log.warn("No order book (Form Binance) stats available for {}", productId);
            return;
        }

        BigDecimal bestPrice = (side == Side.BID) ? stats.getMinAskPrice() : stats.getMaxBidPrice();
        log.info("🔥 Best price for {}: {}", productId, bestPrice);

        String pairId = orderExternalAPI.getPairId(side, order.getGiveCryptoId(), order.getGetCryptoId());

        // 3. Tìm order đối ứng từ Redis (RedisZSet theo chiều ngược lại)
        Side counterSide = (side == Side.BID) ? Side.ASK : Side.BID;
        String redisZSetKey = "orderbook:" + pairId + ":" + counterSide.name().toLowerCase();
        log.info("🔥 Redis ZSet key: {}", redisZSetKey);

        // Lấy order đối ứng có giá tốt nhất từ Redis
        Set<Object> orderRedis = redisTemplate.opsForZSet().range(redisZSetKey, 0, 0);
        log.info("🔥 Order Redis: {}", orderRedis);


        if (orderRedis != null && !orderRedis.isEmpty()) {
            log.info("🔥 Found {} order stats", orderRedis.size());

            String counterOrderKey = (String) orderRedis.iterator().next();

            log.info("🔥 Found counter order key: {}", counterOrderKey);


//            // 4. Get order details from Redis Hash
            String orderJson = (String) redisTemplate.opsForHash().get("order:" + counterOrderKey, "order");
            if (orderJson == null) return;

            Order counterOrder = objectMapper.readValue(orderJson, Order.class);
            log.info("🔥 Counter order: {}", counterOrder);
//
//          // 5. Compare with best price to determine match with user or anonymous
            if ((side == Side.BID && counterOrder.getPrice().compareTo(bestPrice) <= 0) ||
                    (side == Side.ASK && counterOrder.getPrice().compareTo(bestPrice) >= 0)) {

                // ✅ 6. Khớp lệnh giữa 2 user
                match(order, counterOrder);
            } else {
                matchWithAnonymous(order, bestPrice, order.getQuantity());
            }
        } else {
            log.warn("No order redis available for {}", redisZSetKey);
            matchWithAnonymous(order, bestPrice, order.getQuantity());
        }

        // 9. Gửi event lưu giao dịch vào DB hoặc xử lý hậu khớp
    }


    // Xử lý lệnh Limit
    private void handleLimitOrder(Order order) throws JsonProcessingException {
        // 1. Xác định chiều lệnh (BID hoặc ASK)
        // 2. Lấy minAsk & maxAsk hoặc minBid & maxBid từ cache orderBookStatsService
        // 3. Kiểm tra có order đối ứng cùng giá trong Redis OrderBook
        //    - Nếu có: tạo giao dịch thực (khớp toàn phần / một phần)
        //    - Nếu không:
        //        + Nếu giá nằm trong min-max đối ứng => delay random 5-30s rồi khớp với anonymous user
        //        + Nếu giá nằm ngoài min-max => đặt trạng thái PENDING
        // 4. Gửi event tạo giao dịch hoặc cập nhật trạng thái lệnh


        log.info("🔥 Nhận order mới LIMIT: {}", order);


        // 1. Determine the side of the order (BID or ASK)
        Side side = order.getSide();
        String productId = this.getPairIdFromOrderBookData(side, order.getGiveCryptoId(), order.getGetCryptoId());

        // 2. Get stats from cache (continuously updated by BinanceWebSocketService)
        OrderBookStats stats = orderBookStatsService.getStats(productId);
        if (stats == null) {
            log.warn("No order book (Form Binance) stats available for {}", productId);
            return;
        }

        BigDecimal minPrice, maxPrice;
        BigDecimal extendRange = BigDecimal.valueOf(500); // Khoảng mở rộng
        if (side == Side.BID) {
            minPrice = stats.getMinAskPrice().subtract(extendRange);
            maxPrice = stats.getMaxAskPrice().add(extendRange);
        } else {
            minPrice = stats.getMinBidPrice().subtract(extendRange);
            maxPrice = stats.getMaxBidPrice().add(extendRange);
        }

        log.info("🔥 Best price for {}: {} - {}", productId, minPrice, maxPrice);

        Order matchingOrder = null;
        matchingOrder = findMatchingOrderByPrice(order);

        if (matchingOrder != null) {
            log.info("🔥 Tìm thấy order đối ứng cùng giá: {}", matchingOrder);
            match(order, matchingOrder);
        } else {
            log.info("🔥 Không tìm thấy order đối ứng trong Redis");
            // Nếu giá nằm trong khoảng min-max
            if (order.getPrice().compareTo(minPrice) >= 0 && order.getPrice().compareTo(maxPrice) <= 0) {
                // Match with anonymous user after a random delay from 5 to 15 seconds
                scheduleAnonymousMatch(order, Duration.ofSeconds(ThreadLocalRandom.current().nextInt(5, 8)));

            } else {
                // Set PENDING
                log.info("🔥 Order {} nằm ngoài khoảng giá min-max, đặt trạng thái PENDING", order.getId());

                order.setStatus(OrderStatus.PENDING);
                orderExternalAPI.updateOrderStatus(order, BigDecimal.ZERO, BigDecimal.ZERO);
                log.info("🔥 Order {} đã được đặt trạng thái PENDING", order.getId());
            }


        }


        // test các trường hợp
        // TH1: Khớp với order đối ứng cùng giá
        // TH2: Không có order đối ứng cùng giá, nhưng giá nằm trong khoảng min-max => khớp với anonymous user
        // TH3: Không có order đối ứng cùng giá, và giá nằm ngoài khoảng min-max => đặt trạng thái PENDING


        // 9. Gửi event lưu giao dịch vào DB hoặc xử lý hậu khớp;
    }

    // Cập nhật dữ liệu OrderBook từ Binance (OrderBookData)
    public void updateOrderBookData(OrderBookData data) {
        // 1. Cập nhật bestBid và bestAsk vào cache
        // 2. Cập nhật min/max của BID và ASK
        // 3. Nếu có các lệnh LIMIT đang PENDING và giá hiện tại đã vào khoảng min-max
        //    => xét và tạo giao dịch với anonymous user nếu phù hợp
    }


    private void match(Order takerOrder, Order makerOrder) {
        // takerOrder: new order vừa nhận
        // makerOrder: counter order đã tìm thấy từ Redis
        // Nếu makerOrder có side là BID thì isBuyerMaker = true
        System.out.println("🔥 Khớp lệnh: Taker Order: " + takerOrder + ", \nMaker Order: " + makerOrder);

        BigDecimal matchPrice = makerOrder.getPrice();
        BigDecimal matchQuantity = takerOrder.getQuantity().min(makerOrder.getQuantity());
        boolean isBuyerMaker = makerOrder.getSide() == Side.BID;

        // 1. Tạo bản ghi Transaction (Giao dịch)
        Trade trade = Trade.builder()
                .takerOrderId(takerOrder.getId())
                .makerOrderId(makerOrder.getId())
                .productId(orderExternalAPI.getPairId(takerOrder.getSide(), takerOrder.getGiveCryptoId(), takerOrder.getGetCryptoId()))
                .price(makerOrder.getPrice())
                .quantity(matchQuantity)
                .isBuyerMaker(isBuyerMaker)
                .build();
        // Lưu giao dịch vào DB (hoặc gửi event để lưu sau)?
        tradeService.saveTrade(trade);
        log.info("🔥 Đã tạo giao dịch: {}", trade);


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
        log.info("🔥 Cập nhật trạng thái order");
        if (takerOrder.getType() == OrderType.MARKET) {
            takerOrder.setPrice(matchPrice); // Cập nhật giá khớp
        }
        orderExternalAPI.updateOrderStatus(takerOrder, matchQuantity, matchPrice);
        orderExternalAPI.updateOrderStatus(makerOrder, matchQuantity, matchPrice);


        OrderDTO orderDtoTaker = OrderDTO.builder()
                .id(takerOrder.getId())
                .userId(takerOrder.getUserId())
                .pairId(orderExternalAPI.getPairId(takerOrder.getSide(), takerOrder.getGiveCryptoId(), takerOrder.getGetCryptoId()))
                .side(takerOrder.getSide().name())
                .type(takerOrder.getType().name())
                .quantity(takerOrder.getQuantity())
                .price(takerOrder.getPrice())
                .status(takerOrder.getStatus().name())
                .filledQuantity(takerOrder.getFilledQuantity())
                .build();

        OrderDTO orderDtoMaker = OrderDTO.builder()
                .id(makerOrder.getId())
                .userId(makerOrder.getUserId())
                .pairId(orderExternalAPI.getPairId(takerOrder.getSide(), takerOrder.getGiveCryptoId(), takerOrder.getGetCryptoId()))
                .side(makerOrder.getSide().name())
                .type(makerOrder.getType().name())
                .quantity(takerOrder.getQuantity())
                .price(takerOrder.getPrice())
                .status(makerOrder.getStatus().name())
                .filledQuantity(takerOrder.getFilledQuantity())
                .build();

        eventPublisher.publishEvent(new OrderReceivedEvent(orderDtoTaker));
        eventPublisher.publishEvent(new OrderReceivedEvent(orderDtoMaker));

    }

    // Hàm khớp với anonymous user
    private void matchWithAnonymous(Order takerOrder, BigDecimal matchPrice, BigDecimal matchQuantity) {
        log.info("🔥 Khớp lệnh với anonymous user: Order: {}, Price: {}, Quantity: {}", takerOrder, matchPrice, matchQuantity);

        // 1. Tạo Transaction với user ảo
        // 2. Đánh dấu order đã khớp
        // 3. Gửi event khớp lệnh


        boolean isBuyerMaker = takerOrder.getSide() == Side.BID;

        String ANONYMOUS_ORDER_ID = "anonymous-order";
        Trade trade = Trade.builder()
                .takerOrderId(takerOrder.getId())
                .makerOrderId(ANONYMOUS_ORDER_ID)
                .productId(orderExternalAPI.getPairId(takerOrder.getSide(), takerOrder.getGiveCryptoId(), takerOrder.getGetCryptoId()))
                .price(matchPrice)
                .quantity(matchQuantity)
                .isBuyerMaker(isBuyerMaker)
                .build();

        tradeService.saveTrade(trade);
        log.info("🔥 Đã tạo giao dịch với anonymous user: {}", trade);
        // Giao dịch với anonymous user luôn là FILLED
        takerOrder.setStatus(OrderStatus.FILLED);
        orderExternalAPI.updateOrderStatus(takerOrder, matchQuantity, matchPrice);

        OrderDTO orderDtoTaker = OrderDTO.builder()
                .id(takerOrder.getId())
                .userId(takerOrder.getUserId())
                .pairId(orderExternalAPI.getPairId(takerOrder.getSide(), takerOrder.getGiveCryptoId(), takerOrder.getGetCryptoId()))
                .side(takerOrder.getSide().name())
                .type(takerOrder.getType().name())
                .quantity(takerOrder.getQuantity())
                .price(takerOrder.getPrice())
                .status(takerOrder.getStatus().name())
                .filledQuantity(takerOrder.getFilledQuantity())
                .build();
        eventPublisher.publishEvent(new OrderReceivedEvent(orderDtoTaker));

    }

    // Hàm delay khớp với anonymous sau 5-30s
    private void scheduleAnonymousMatch(Order order, Duration delay) {
        // 1. Sử dụng ScheduledExecutorService hoặc TaskScheduler để delay
        // 2. Sau delay, kiểm tra lại khoảng giá và khớp nếu hợp lệ
        scheduler.schedule(() -> {
            try {

                log.info("⏳ Khớp anonymous cho order {} sau {} giây", order.getId(), delay.toSeconds());

                // Kiểm tra lại giá trước khi khớp (tránh khớp sai khi thị trường đã thay đổi)
                Order matchingOrder = findMatchingOrderByPrice(order);
                if (matchingOrder != null) {
                    log.info("🔥 Tìm thấy order đối ứng trong lúc delay: {}", matchingOrder);
                    match(order, matchingOrder);
                } else {
                    // Nếu vẫn không có order thật => khớp với anonymous user
                    log.info("🔥 Khớp với anonymous user");
                    log.info("🔥Khớp với anonymous user (Không tìm thấy order đối ứng");
                    matchWithAnonymous(order, order.getPrice(), order.getQuantity());
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }, delay.toMillis(), TimeUnit.MILLISECONDS);

    }

    // Hàm kiểm tra order limit PENDING có nên khớp với anonymous không
    private void checkPendingOrdersAgainstOrderBook() {
        // 1. Lấy danh sách các order LIMIT có trạng thái PENDING
        // 2. So sánh khoảng giá hiện tại với price của từng order
        // 3. Nếu vào vùng min-max => khớp với anonymous user
    }


    /*--------------- Hàm tiện ích -------------------------------------------------------------------------------*/


    public String getPairIdFromOrderBookData(Side side, String giveCryptoId, String getCryptoId) {
        return side == Side.BID ?
                getCryptoId + giveCryptoId :
                giveCryptoId + getCryptoId;
    }

    private Order findMatchingOrderByPrice(Order order) throws JsonProcessingException {
        // Determining the counter side based on the order side
        Side counterSide = (order.getSide() == Side.BID) ? Side.ASK : Side.BID;
        String pairId = orderExternalAPI.getPairId(order.getSide(), order.getGiveCryptoId(), order.getGetCryptoId());
        String redisZSetKey = "orderbook:" + pairId + ":" + counterSide.name().toLowerCase();

        // Calculate the price part based on the order side
        double pricePart = (counterSide == Side.BID)
                ? -order.getPrice().doubleValue()
                : order.getPrice().doubleValue();

        // Determine the score range for the search
        // Using a small epsilon to allow for slight variations in price matching
        double epsilon = 0.5;
        double scoreMin = pricePart - epsilon;
        double scoreMax = pricePart + epsilon;

        System.out.println("🔥 Tìm kiếm order đối ứng trong Redis ZSet: " + redisZSetKey +
                ", Score Min: " + scoreMin + ", Score Max: " + scoreMax);

        // Fetching the matching order from Redis ZSet
        Set<Object> orderRedis = redisTemplate.opsForZSet()
                .rangeByScore(redisZSetKey, scoreMin, scoreMax, 0, 1);

        if (orderRedis != null && !orderRedis.isEmpty()) {
            String counterOrderKey = (String) orderRedis.iterator().next();
            System.out.println("🔥 Found counter order key: " + counterOrderKey);

            // Fetching the order details from Redis Hash
            String orderJson = (String) redisTemplate.opsForHash().get("order:" + counterOrderKey, "order");
            if (orderJson == null) return null;

            return objectMapper.readValue(orderJson, Order.class);

        }
        return null;
    }


}
