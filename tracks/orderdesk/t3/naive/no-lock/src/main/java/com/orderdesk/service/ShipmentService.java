package com.orderdesk.service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderdesk.domain.Order;
import com.orderdesk.domain.OrderLine;
import com.orderdesk.domain.OrderStatus;
import com.orderdesk.domain.Shipment;
import com.orderdesk.domain.ShipmentLine;
import com.orderdesk.repository.OrderRepository;
import com.orderdesk.repository.ShipmentRepository;

/**
 * Partial shipments. The order row is locked for the whole operation, so the "already shipped" check and the
 * new shipment commit atomically; stock rows are locked by {@link InventoryService} as before.
 */
@Service
public class ShipmentService {

    private final OrderRepository orders;
    private final ShipmentRepository shipments;
    private final InventoryService inventory;
    private final Clock clock;

    public ShipmentService(OrderRepository orders, ShipmentRepository shipments, InventoryService inventory, Clock clock) {
        this.orders = orders;
        this.shipments = shipments;
        this.inventory = inventory;
        this.clock = clock;
    }

    public record Requested(String sku, int quantity) {
    }

    @Transactional
    public Shipment ship(long orderId, List<Requested> requested) {
        Order order = lockOrder(orderId);
        Map<String, Integer> remaining = remaining(order);
        Map<String, Integer> wanted = new LinkedHashMap<>();
        for (Requested r : requested) {
            if (wanted.put(r.sku(), r.quantity()) != null) {
                throw invalid("Duplicate sku " + r.sku() + " in shipment");
            }
            Integer left = remaining.get(r.sku());
            if (left == null) {
                throw invalid("Order " + orderId + " has no line for " + r.sku());
            }
            if (r.quantity() > left) {
                throw invalid("Only " + left + " of " + r.sku() + " left to ship");
            }
        }
        return record(order, wanted, remaining);
    }

    /** v1 behaviour: ship everything not shipped yet, as one shipment. */
    @Transactional
    public Order shipRemaining(long orderId) {
        Order order = lockOrder(orderId);
        Map<String, Integer> remaining = remaining(order);
        Map<String, Integer> wanted = new LinkedHashMap<>();
        remaining.forEach((sku, left) -> {
            if (left > 0) {
                wanted.put(sku, left);
            }
        });
        record(order, wanted, remaining);
        return order;
    }

    private Order lockOrder(long orderId) {
        Order order = orders.findWithLinesById(orderId).orElseThrow(() -> BusinessException.notFound("order", orderId));
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PARTIALLY_SHIPPED) {
            throw BusinessException.invalidState("Only paid orders can be shipped");
        }
        return order;
    }

    private Map<String, Integer> remaining(Order order) {
        Map<String, Integer> left = new LinkedHashMap<>();
        for (OrderLine line : order.getLines()) {
            left.put(line.getSku(), line.getQuantity());
        }
        Map<String, Integer> shipped = new HashMap<>();
        for (Shipment s : shipments.findByOrderIdWithLines(order.getId())) {
            for (ShipmentLine l : s.getLines()) {
                shipped.merge(l.getSku(), l.getQuantity(), Integer::sum);
            }
        }
        shipped.forEach((sku, q) -> left.computeIfPresent(sku, (k, v) -> v - q));
        return left;
    }

    private Shipment record(Order order, Map<String, Integer> wanted, Map<String, Integer> remaining) {
        Instant now = Instant.now(clock);
        Shipment shipment = new Shipment(order.getId(), now);
        wanted.forEach((sku, quantity) -> {
            inventory.ship(sku, quantity);
            shipment.getLines().add(new ShipmentLine(sku, quantity));
            remaining.computeIfPresent(sku, (k, v) -> v - quantity);
        });
        boolean complete = remaining.values().stream().allMatch(v -> v == 0);
        order.setStatus(complete ? OrderStatus.SHIPPED : OrderStatus.PARTIALLY_SHIPPED);
        if (complete) {
            order.setShippedAt(now);
        }
        return shipments.save(shipment);
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "SHIPMENT_INVALID", message);
    }
}
