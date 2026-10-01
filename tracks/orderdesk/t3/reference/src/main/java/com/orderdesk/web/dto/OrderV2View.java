package com.orderdesk.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.orderdesk.domain.Order;
import com.orderdesk.domain.Shipment;
import com.orderdesk.domain.ShipmentLine;

/** v2 order: v1 fields plus per-line shipped quantity and the shipments themselves. */
public record OrderV2View(Long id, String customerId, String status, Instant createdAt, BigDecimal total,
        BigDecimal paidAmount, BigDecimal refundedAmount, Instant shippedAt, List<Line> lines, List<ShipmentView> shipments) {

    public record Line(String sku, int quantity, int shippedQuantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    public static OrderV2View of(Order o, List<Shipment> shipments) {
        Map<String, Integer> shipped = new HashMap<>();
        for (Shipment s : shipments) {
            for (ShipmentLine l : s.getLines()) {
                shipped.merge(l.getSku(), l.getQuantity(), Integer::sum);
            }
        }
        List<Line> lines = o.getLines().stream().map(l -> new Line(l.getSku(), l.getQuantity(),
                shipped.getOrDefault(l.getSku(), 0), l.getUnitPrice(), l.getLineTotal())).toList();
        return new OrderV2View(o.getId(), o.getCustomerId(), o.getStatus().name(), o.getCreatedAt(), o.getTotal(),
                o.getPaidAmount(), o.getRefundedAmount(), o.getShippedAt(), lines, shipments.stream().map(ShipmentView::of).toList());
    }
}
