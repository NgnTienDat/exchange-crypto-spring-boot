package com.ntd.exchange_crypto.trade.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

    public MatchEngine(TradeService tradeService,
                       OrderBookStatsService orderBookStatsService,
                       OrderExternalAPI orderExternalAPI,
                       ApplicationEventPublisher eventPublisher, RedisTemplate<String,
                    Object> redisTemplate, ObjectMapper objectMapper, OrderMapper orderMapper) {
        this.tradeService = tradeService;
        this.orderBookStatsService = orderBookStatsService;
        this.orderExternalAPI = orderExternalAPI;
        this.eventPublisher = eventPublisher;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;

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


    private void handleMarketOrder(Order order) throws JsonProcessingException {
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

        // ---- Logic mới ----
        BigDecimal remainingQty = order.getQuantity();
        List<Order> matchedCounterOrders = new ArrayList<>();

        // Lấy nhiều lệnh từ Redis (ví dụ lấy top 50)
        Set<Object> counterOrders = redisTemplate.opsForZSet().range(redisZSetKey, 0, 49);

        if (counterOrders != null && !counterOrders.isEmpty()) {
            log.info("🔥 Found {} counter orders in Redis", counterOrders.size());
            int counterOrderCount = 0;
            for (Object keyObj : counterOrders) {
                String counterOrderKey = (String) keyObj;
                String orderJson = (String) redisTemplate.opsForHash().get("order:" + counterOrderKey, "order");
                if (orderJson == null) continue;

                Order counterOrder = objectMapper.readValue(orderJson, Order.class);

                if (counterOrder.getUserId().equals(order.getUserId())) {
                    // Không khớp lệnh với chính mình
                    continue;
                }

                // Check giá trước khi match
                if ((side == Side.BID && counterOrder.getPrice().compareTo(bestPrice) <= 0) ||
                        (side == Side.ASK && counterOrder.getPrice().compareTo(bestPrice) >= 0)) {

                    matchedCounterOrders.add(counterOrder);
                    log.info("Khớp lệnh lần: {}", ++counterOrderCount);

                    // Trừ dần quantity
                    if (remainingQty.compareTo(counterOrder.getQuantity()) > 0) {
                        remainingQty = remainingQty.subtract(counterOrder.getQuantity());
                    } else {
                        remainingQty = BigDecimal.ZERO;
                        break;
                    }
                } else {
                    // nếu giá không phù hợp thì break luôn (vì RedisZSet đã sắp theo giá)
                    break;
                }
            }
        }

        // Nếu tìm được counterOrders thì match
        if (!matchedCounterOrders.isEmpty()) {
            match(order, matchedCounterOrders);
        }

        // Nếu còn dư -> match với anonymous
        if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            matchWithAnonymous(order, bestPrice, remainingQty);
        }
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

        List<Order> matchingOrders = findMatchingOrdersByPrice(order);
        if (!matchingOrders.isEmpty()) {
            // Gom quantity
            BigDecimal totalCounterQty = matchingOrders.stream()
                    .map(o -> o.getQuantity().subtract(
                            o.getFilledQuantity() != null ? o.getFilledQuantity() : BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            log.info("totalCounterQty: {}, order quantity: {}", totalCounterQty, order.getQuantity());

            if (totalCounterQty.compareTo(order.getQuantity()) >= 0) {
                log.info("🔥 Tìm thấy đủ counter orders cùng giá để khớp: {}", matchingOrders.size());
                match(order, matchingOrders); // khớp lần lượt
            } else {
                log.info("🔥 Counter orders cùng giá chưa đủ quantity, order còn lại sẽ PENDING");
                // khớp phần có thể -> sau đó set PENDING cho phần còn lại
                match(order, matchingOrders);
                order.setStatus(OrderStatus.PENDING);
                orderExternalAPI.updateOrderStatus(order,
                        order.getQuantity().subtract(order.getFilledQuantity()),
                        order.getPrice());
            }
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
                OrderDTO orderSendToAdmin = OrderDTO.builder()
                        .id(order.getId())
                        .userId("1218a33f-e5dd-4e4b-8589-8a53c4d0144d")
                        .pairId(orderExternalAPI.getPairId(order.getSide(), order.getGiveCryptoId(), order.getGetCryptoId()))
                        .side(order.getSide().name())
                        .type(order.getType().name())
                        .quantity(order.getQuantity())
                        .price(order.getPrice())
                        .status(order.getStatus().name())
                        .filledQuantity(order.getFilledQuantity())
                        .build();

                // send to admin
                eventPublisher.publishEvent(new OrderReceivedEvent(orderSendToAdmin));
            }


        }


        // test các trường hợp
        // TH1: Khớp với order đối ứng cùng giá
        // TH2: Không có order đối ứng cùng giá, nhưng giá nằm trong khoảng min-max => khớp với anonymous user
        // TH3: Không có order đối ứng cùng giá, và giá nằm ngoài khoảng min-max => đặt trạng thái PENDING


        // 9. Gửi event lưu giao dịch vào DB hoặc xử lý hậu khớp;
    }


    private void matchZ(Order takerOrder, Order makerOrder) {
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
        log.info("🔥 Cập nhật trạng thái taker: {} và maker: {} là FILLED",
                takerOrder.getStatus(), makerOrder.getStatus());

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

    private void match(Order takerOrder, List<Order> makerOrders) {
        // Lấy quantity còn lại của taker (chưa khớp hết)
        BigDecimal remainingTakerQty = takerOrder.getQuantity();
        BigDecimal takerFilled = takerOrder.getFilledQuantity() != null ?
                takerOrder.getFilledQuantity() : BigDecimal.ZERO;

        log.info("🔥 Bắt đầu khớp lệnh: Taker {} với {} Maker orders", takerOrder.getId(), makerOrders.size());

        for (Order makerOrder : makerOrders) {
            // Nếu taker đã khớp xong thì dừng
            if (remainingTakerQty.compareTo(BigDecimal.ZERO) <= 0) break;

            // Quantity còn lại của maker = quantity gốc - đã filled
            BigDecimal makerRemaining = makerOrder.getQuantity().subtract(
                    makerOrder.getFilledQuantity() != null ? makerOrder.getFilledQuantity() : BigDecimal.ZERO
            );

            // Nếu maker không còn quantity thì bỏ qua
            if (makerRemaining.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Xác định quantity có thể khớp = min(takerRemaining, makerRemaining)
            BigDecimal matchQuantity = remainingTakerQty.min(makerRemaining);
            BigDecimal matchPrice = makerOrder.getPrice(); // Giá lấy từ maker
            boolean isBuyerMaker = makerOrder.getSide() == Side.BID;

            Trade trade = Trade.builder()
                    .takerOrderId(takerOrder.getId())
                    .makerOrderId(makerOrder.getId())
                    .productId(orderExternalAPI.getPairId(
                            takerOrder.getSide(), takerOrder.getGiveCryptoId(), takerOrder.getGetCryptoId()
                    ))
                    .price(matchPrice)
                    .quantity(matchQuantity)
                    .isBuyerMaker(isBuyerMaker)
                    .build();

            tradeService.saveTrade(trade);
            log.info("🔥 Đã tạo giao dịch: {}", trade);

            takerFilled = takerFilled.add(matchQuantity);
            takerOrder.setFilledQuantity(takerFilled);

            BigDecimal makerFilled = makerOrder.getFilledQuantity() != null ?
                    makerOrder.getFilledQuantity() : BigDecimal.ZERO;
            makerOrder.setFilledQuantity(makerFilled.add(matchQuantity));

            // ==================== 3. Cập nhật trạng thái ====================
            if (takerOrder.getFilledQuantity().compareTo(takerOrder.getQuantity()) >= 0) {
                takerOrder.setStatus(OrderStatus.FILLED);
            } else {
                takerOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
            }

            if (makerOrder.getFilledQuantity().compareTo(makerOrder.getQuantity()) >= 0) {
                makerOrder.setStatus(OrderStatus.FILLED);
            } else {
                makerOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
            }

            // Với lệnh MARKET thì giá cuối cùng = giá của maker vừa khớp
            if (takerOrder.getType() == OrderType.MARKET) {
                takerOrder.setPrice(matchPrice);
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
                    .quantity(makerOrder.getQuantity())
                    .price(makerOrder.getPrice())
                    .status(makerOrder.getStatus().name())
                    .filledQuantity(makerOrder.getFilledQuantity())
                    .build();



            // Bắn event cập nhật trạng thái của cả taker & maker
            // send to user
            eventPublisher.publishEvent(new OrderReceivedEvent(orderDtoTaker));
            eventPublisher.publishEvent(new OrderReceivedEvent(orderDtoMaker));


            // send to admin
//            orderDtoMaker.setUserId("1218a33f-e5dd-4e4b-8589-8a53c4d0144d");
//            orderDtoTaker.setUserId("1218a33f-e5dd-4e4b-8589-8a53c4d0144d");
//
//            eventPublisher.publishEvent(new OrderReceivedEvent(orderDtoTaker));
//            eventPublisher.publishEvent(new OrderReceivedEvent(orderDtoMaker));



            // ==================== 5. Giảm remainingQty của taker ====================
            remainingTakerQty = remainingTakerQty.subtract(matchQuantity);
        }

        log.info("🔥 Hoàn tất khớp lệnh: Taker {} status={}, filled={}/{}",
                takerOrder.getId(),
                takerOrder.getStatus(),
                takerOrder.getFilledQuantity(),
                takerOrder.getQuantity()
        );
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

//        orderDtoTaker.setUserId("1218a33f-e5dd-4e4b-8589-8a53c4d0144d");
//
//        eventPublisher.publishEvent(new OrderReceivedEvent(orderDtoTaker));

    }

    // Hàm delay khớp với anonymous sau 5-30s
    private void scheduleAnonymousMatch(Order order, Duration delay) {
        // 1. Sử dụng ScheduledExecutorService hoặc TaskScheduler để delay
        // 2. Sau delay, kiểm tra lại khoảng giá và khớp nếu hợp lệ
        scheduler.schedule(() -> {
            try {

                log.info("⏳ Khớp anonymous cho order {} sau {} giây", order.getId(), delay.toSeconds());

                // Kiểm tra lại giá trước khi khớp (tránh khớp sai khi thị trường đã thay đổi)
                List<Order> matchingOrders = findMatchingOrdersByPrice(order);
                if (!matchingOrders.isEmpty()) {
                    log.info("🔥 Tìm thấy order đối ứng trong lúc delay: {}", matchingOrders.size());
                    match(order, matchingOrders);
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


    /*--------------- Hàm tiện ích -------------------------------------------------------------------------------*/


    public String getPairIdFromOrderBookData(Side side, String giveCryptoId, String getCryptoId) {
        return side == Side.BID ?
                getCryptoId + giveCryptoId :
                giveCryptoId + getCryptoId;
    }

    private List<Order> findMatchingOrdersByPrice(Order order) throws JsonProcessingException {
        Side counterSide = (order.getSide() == Side.BID) ? Side.ASK : Side.BID;
        String pairId = orderExternalAPI.getPairId(order.getSide(), order.getGiveCryptoId(), order.getGetCryptoId());
        String redisZSetKey = "orderbook:" + pairId + ":" + counterSide.name().toLowerCase();

        double pricePart = (counterSide == Side.BID)
                ? -order.getPrice().doubleValue()
                : order.getPrice().doubleValue();

        // Cho phép ±epsilon khi so sánh giá (nếu cần)
        double epsilon = 0.5;
        double scoreMin = pricePart - epsilon;
        double scoreMax = pricePart + epsilon;

        System.out.println("🔥 Tìm kiếm nhiều order đối ứng trong Redis ZSet: " + redisZSetKey +
                ", Score Min: " + scoreMin + ", Score Max: " + scoreMax);

        // Lấy tất cả order cùng giá
        Set<Object> orderRedis = redisTemplate.opsForZSet()
                .rangeByScore(redisZSetKey, scoreMin, scoreMax);

        List<Order> counterOrders = new ArrayList<>();
        if (orderRedis != null) {
            for (Object keyObj : orderRedis) {
                String counterOrderKey = (String) keyObj;
                String orderJson = (String) redisTemplate.opsForHash().get("order:" + counterOrderKey, "order");
                if (orderJson != null) {
                    Order counterOrder = objectMapper.readValue(orderJson, Order.class);
                    if (counterOrder.getUserId().equals(order.getUserId())) {
                        // Không khớp lệnh với chính mình
                        continue;
                    }
                    counterOrders.add(counterOrder);
                }
            }
        }

        return counterOrders;
    }


}
