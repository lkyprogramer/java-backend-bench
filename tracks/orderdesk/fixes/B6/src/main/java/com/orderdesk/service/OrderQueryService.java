package com.orderdesk.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderdesk.repository.OrderRepository;
import com.orderdesk.web.dto.OrderSummary;
import com.orderdesk.web.dto.OrderView;

@Service
public class OrderQueryService {

    private final OrderRepository orders;

    public OrderQueryService(OrderRepository orders) {
        this.orders = orders;
    }

    /** The view is built inside the transaction; entities never reach the web layer (open-in-view is off). */
    @Transactional(readOnly = true)
    public OrderView get(long orderId) {
        return OrderView.of(orders.findWithLinesById(orderId).orElseThrow(() -> BusinessException.notFound("order", orderId)));
    }

    @Transactional(readOnly = true)
    public List<OrderSummary> listForCustomer(String customerId) {
        return orders.findByCustomerWithLines(customerId).stream().map(OrderSummary::of).toList();
    }
}
