package com.orderdesk.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderdesk.domain.Order;
import com.orderdesk.domain.OrderStatus;
import com.orderdesk.domain.PaymentAttempt;
import com.orderdesk.payment.PaymentDeclinedException;
import com.orderdesk.payment.PaymentGateway;
import com.orderdesk.repository.OrderRepository;
import com.orderdesk.repository.PaymentAttemptRepository;
import com.orderdesk.web.dto.OrderView;

@Service
public class PaymentService {

    private final OrderRepository orders;
    private final PaymentAttemptRepository attempts;
    private final PaymentGateway gateway;
    private final Clock clock;

    public PaymentService(OrderRepository orders, PaymentAttemptRepository attempts, PaymentGateway gateway, Clock clock) {
        this.orders = orders;
        this.attempts = attempts;
        this.gateway = gateway;
        this.clock = clock;
    }

    @Transactional
    public OrderView pay(long orderId, String cardNumber) throws PaymentDeclinedException {
        Order order = orders.findById(orderId).orElseThrow(() -> BusinessException.notFound("order", orderId));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw BusinessException.invalidState("Order is not awaiting payment");
        }
        order.setStatus(OrderStatus.PAID);
        order.setPaidAmount(order.getTotal());
        try {
            gateway.charge(cardNumber, order.getTotal());
        } catch (PaymentDeclinedException e) {
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            throw e;
        }
        attempts.save(new PaymentAttempt(order.getId(), order.getTotal(), "SUCCEEDED", Instant.now(clock)));
        return OrderView.of(order);
    }
}
