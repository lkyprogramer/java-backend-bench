package com.orderdesk.service;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderdesk.domain.Order;
import com.orderdesk.domain.OrderStatus;
import com.orderdesk.repository.OrderRepository;
import com.orderdesk.web.dto.OrderView;

@Service
public class RefundService {

    private static final Set<OrderStatus> REFUNDABLE = EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.PARTIALLY_REFUNDED);

    private final OrderRepository orders;

    public RefundService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional
    public OrderView refund(long orderId, BigDecimal amount) {
        Order order = orders.findById(orderId).orElseThrow(() -> BusinessException.notFound("order", orderId));
        if (!REFUNDABLE.contains(order.getStatus())) {
            throw BusinessException.invalidState("Order cannot be refunded in status " + order.getStatus());
        }
        if (amount.signum() <= 0 || amount.scale() > 2) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Refund amount must be positive with at most 2 decimals");
        }
        BigDecimal remaining = order.getPaidAmount().subtract(order.getRefundedAmount()).subtract(amount);
        if (remaining.signum() < 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "REFUND_EXCEEDS_PAYMENT", "Refund exceeds the paid amount");
        }
        order.setRefundedAmount(order.getRefundedAmount().add(amount));
        order.setStatus(remaining.signum() == 0 ? OrderStatus.REFUNDED : OrderStatus.PARTIALLY_REFUNDED);
        return OrderView.of(order);
    }
}
