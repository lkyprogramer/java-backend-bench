package com.orderdesk.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.orderdesk.domain.Order;

public record OrderSummary(Long id, String status, BigDecimal total, int lineCount, Instant createdAt) {

    public static OrderSummary of(Order o) {
        return new OrderSummary(o.getId(), o.getStatus().name(), o.getTotal(), o.getLines().size(), o.getCreatedAt());
    }
}
