package com.orderdesk.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderdesk.config.OrderDeskProperties;
import com.orderdesk.domain.Order;
import com.orderdesk.domain.OrderLine;
import com.orderdesk.domain.OrderStatus;
import com.orderdesk.domain.Product;
import com.orderdesk.repository.OrderRepository;
import com.orderdesk.repository.ProductRepository;
import com.orderdesk.web.dto.OrderView;
import com.orderdesk.web.dto.PlaceOrderRequest;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final ProductRepository products;
    private final InventoryService inventory;
    private final OrderPricing pricing;
    private final OrderDeskProperties properties;
    private final Clock clock;

    public OrderService(OrderRepository orders, ProductRepository products, InventoryService inventory,
            OrderPricing pricing, OrderDeskProperties properties, Clock clock) {
        this.orders = orders;
        this.products = products;
        this.inventory = inventory;
        this.pricing = pricing;
        this.properties = properties;
        this.clock = clock;
    }

    public OrderView placeOrder(PlaceOrderRequest request) {
        List<PlaceOrderRequest.Line> lines = request.lines().stream()
                .sorted(Comparator.comparing(PlaceOrderRequest.Line::sku)).toList();
        if (lines.stream().map(PlaceOrderRequest.Line::sku).distinct().count() != lines.size()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Duplicate sku in order lines");
        }
        return OrderView.of(createOrder(request.customerId(), lines));
    }

    @Transactional
    public Order createOrder(String customerId, List<PlaceOrderRequest.Line> lines) {
        Order order = new Order(customerId, Instant.now(clock));
        for (PlaceOrderRequest.Line line : lines) {
            Product product = products.findById(line.sku()).orElseThrow(() -> BusinessException.notFound("product", line.sku()));
            try {
                inventory.reserve(line.sku(), line.quantity());
            } catch (InsufficientStockException e) {
                // give back what this order already reserved
                for (OrderLine done : order.getLines()) {
                    inventory.release(done.getSku(), done.getQuantity());
                }
                throw e;
            }
            order.addLine(new OrderLine(line.sku(), line.quantity(), product.getPrice()));
        }
        if (order.getTotal().compareTo(properties.maxOrderAmount()) > 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "ORDER_LIMIT_EXCEEDED",
                    "Order total " + order.getTotal() + " exceeds " + properties.maxOrderAmount());
        }
        if (!pricing.requiresPayment(order.getTotal())) {
            order.setStatus(OrderStatus.PAID);
        }
        return orders.save(order);
    }

    @Transactional
    public OrderView ship(long orderId) {
        Order order = load(orderId);
        if (order.getStatus() != OrderStatus.PAID) {
            throw BusinessException.invalidState("Only paid orders can be shipped");
        }
        for (OrderLine line : order.getLines()) {
            inventory.ship(line.getSku(), line.getQuantity());
        }
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(Instant.now(clock));
        return OrderView.of(order);
    }

    @Transactional
    public OrderView cancel(long orderId) {
        Order order = load(orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw BusinessException.invalidState("Only unpaid orders can be cancelled");
        }
        for (OrderLine line : order.getLines()) {
            inventory.release(line.getSku(), line.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
        return OrderView.of(order);
    }

    private Order load(long orderId) {
        return orders.findById(orderId).orElseThrow(() -> BusinessException.notFound("order", orderId));
    }
}
