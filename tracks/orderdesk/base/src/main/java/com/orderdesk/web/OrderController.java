package com.orderdesk.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.orderdesk.payment.PaymentDeclinedException;
import com.orderdesk.service.OrderQueryService;
import com.orderdesk.service.OrderService;
import com.orderdesk.service.PaymentService;
import com.orderdesk.service.RefundService;
import com.orderdesk.web.dto.OrderSummary;
import com.orderdesk.web.dto.OrderView;
import com.orderdesk.web.dto.PlaceOrderRequest;
import com.orderdesk.web.dto.Requests;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;
    private final OrderQueryService queries;
    private final PaymentService payments;
    private final RefundService refunds;

    public OrderController(OrderService orders, OrderQueryService queries, PaymentService payments, RefundService refunds) {
        this.orders = orders;
        this.queries = queries;
        this.payments = payments;
        this.refunds = refunds;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView place(@Valid @RequestBody PlaceOrderRequest request) {
        return orders.placeOrder(request);
    }

    @GetMapping("/{id}")
    public OrderView get(@PathVariable long id) {
        return OrderView.of(queries.find(id));
    }

    @GetMapping
    public List<OrderSummary> list(@RequestParam String customerId) {
        return queries.listForCustomer(customerId);
    }

    @PostMapping("/{id}/pay")
    public OrderView pay(@PathVariable long id, @Valid @RequestBody Requests.Pay request) throws PaymentDeclinedException {
        return payments.pay(id, request.cardNumber());
    }

    @PostMapping("/{id}/ship")
    public OrderView ship(@PathVariable long id) {
        return orders.ship(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderView cancel(@PathVariable long id) {
        return orders.cancel(id);
    }

    @PostMapping("/{id}/refunds")
    public OrderView refund(@PathVariable long id, @Valid @RequestBody Requests.Refund request) {
        return refunds.refund(id, request.amount());
    }
}
