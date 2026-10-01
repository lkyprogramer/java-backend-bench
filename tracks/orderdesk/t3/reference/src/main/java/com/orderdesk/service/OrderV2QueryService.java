package com.orderdesk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderdesk.repository.OrderRepository;
import com.orderdesk.repository.ShipmentRepository;
import com.orderdesk.web.dto.OrderV2View;
import com.orderdesk.web.dto.ShipmentView;

@Service
public class OrderV2QueryService {

    private final OrderRepository orders;
    private final ShipmentRepository shipments;

    public OrderV2QueryService(OrderRepository orders, ShipmentRepository shipments) {
        this.orders = orders;
        this.shipments = shipments;
    }

    @Transactional(readOnly = true)
    public OrderV2View get(long orderId) {
        var order = orders.findWithLinesById(orderId).orElseThrow(() -> BusinessException.notFound("order", orderId));
        return OrderV2View.of(order, shipments.findByOrderIdWithLines(orderId));
    }

    @Transactional(readOnly = true)
    public ShipmentView shipment(long shipmentId) {
        return ShipmentView.of(shipments.findById(shipmentId).orElseThrow(() -> BusinessException.notFound("shipment", shipmentId)));
    }
}
