package com.orderdesk.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.orderdesk.domain.Order;
import com.orderdesk.domain.OrderLine;

public record OrderView(Long id, String customerId, String status, Instant createdAt, BigDecimal total,
        BigDecimal paidAmount, BigDecimal refundedAmount, Instant shippedAt, List<LineView> lines) {

    public record LineView(String sku, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {

        static LineView of(OrderLine l) {
            return new LineView(l.getSku(), l.getQuantity(), l.getUnitPrice(), l.getLineTotal());
        }
    }

    public static OrderView of(Order o) {
        return new OrderView(o.getId(), o.getCustomerId(), o.getStatus().name(), o.getCreatedAt(), o.getTotal(),
                o.getPaidAmount(), o.getRefundedAmount(), o.getShippedAt(), o.getLines().stream().map(LineView::of).toList());
    }
}
