package com.ntd.exchange_crypto.order.service;

import com.ntd.exchange_crypto.order.enums.OrderStatus;
import com.ntd.exchange_crypto.order.enums.OrderType;
import com.ntd.exchange_crypto.order.enums.Side;
import com.ntd.exchange_crypto.order.exception.OrderErrorCode;
import com.ntd.exchange_crypto.order.exception.OrderException;
import com.ntd.exchange_crypto.order.model.Order;
import com.ntd.exchange_crypto.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderStatisticsService {

    private final OrderRepository orderRepository;


    public List<Order> getTodayOrders() {
        return orderRepository.findTodayOrders();
    }

    public List<Order> getOrdersByMonth(int year, int month) {
        return orderRepository.findByYearAndMonth(year, month);
    }

    public List<Order> getOrdersByYear(int year) {
        return orderRepository.findByYear(year);
    }

    // Convenience methods for current periods


    public List<Order> getCurrentMonthOrders() {
        LocalDate now = LocalDate.now();
        return getOrdersByMonth(now.getYear(), now.getMonthValue());
    }

    public List<Order> getCurrentYearOrders() {
        LocalDate now = LocalDate.now();
        return getOrdersByYear(now.getYear());
    }

    // Basic statistics methods

    public long countTodayOrders() {
        return getTodayOrders().size();
    }

    public long countOrdersByMonth(int month, int year) {
        if (month < 1 || month > 12 ) {
            throw new OrderException(OrderErrorCode.INVALID_MONTH);
        }
        if (year < 1970 || year > 2100) {
            throw new OrderException(OrderErrorCode.INVALID_YEAR);
        }

        return getOrdersByMonth(year, month).size();
    }

    public long countOrdersByYear(int year) {
        if (year < 1970 || year > 2100) {
            throw new OrderException(OrderErrorCode.INVALID_YEAR);
        }
        return getOrdersByYear(year).size();
    }

}