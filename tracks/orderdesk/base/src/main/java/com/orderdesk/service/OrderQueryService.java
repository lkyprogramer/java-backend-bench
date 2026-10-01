package com.orderdesk.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderdesk.domain.Order;
import com.orderdesk.repository.OrderRepository;
import com.orderdesk.web.dto.OrderSummary;

@Service
public class OrderQueryService {

    private final OrderRepository orders;

    public OrderQueryService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public Order find(long orderId) {
        return orders.findById(orderId).orElseThrow(() -> BusinessException.notFound("order", orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderSummary> listForCustomer(String customerId) {
        return orders.findByCustomerWithLines(customerId).stream().map(OrderSummary::of).toList();
    }
}
