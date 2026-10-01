package com.orderdesk.web.dto;

import java.time.Instant;
import java.util.List;

import com.orderdesk.domain.Shipment;

public record ShipmentView(Long id, Long orderId, Instant shippedAt, List<Line> lines) {

    public record Line(String sku, int quantity) {
    }

    public static ShipmentView of(Shipment s) {
        return new ShipmentView(s.getId(), s.getOrderId(), s.getShippedAt(),
                s.getLines().stream().map(l -> new Line(l.getSku(), l.getQuantity())).toList());
    }
}
