package com.orderdesk.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderdesk.config.OrderDeskProperties;
import com.orderdesk.domain.Order;
import com.orderdesk.domain.OrderStatus;
import com.orderdesk.repository.OrderRepository;
import com.orderdesk.web.dto.DailyReport;

@Service
public class ReportService {

    private final OrderRepository orders;
    private final OrderDeskProperties properties;

    public ReportService(OrderRepository orders, OrderDeskProperties properties) {
        this.orders = orders;
        this.properties = properties;
    }

    /** Orders placed during the given business day, excluding cancelled ones. */
    @Transactional(readOnly = true)
    public DailyReport daily(LocalDate date) {
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        Instant from = date.atStartOfDay(zone).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
        List<Order> placed = orders.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to).stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED).toList();
        BigDecimal gross = placed.stream().map(Order::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DailyReport(date, properties.businessZone().getId(), placed.size(), gross);
    }
}
