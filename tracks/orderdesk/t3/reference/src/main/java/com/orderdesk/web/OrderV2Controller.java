package com.orderdesk.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.orderdesk.service.OrderV2QueryService;
import com.orderdesk.service.ShipmentService;
import com.orderdesk.web.dto.CreateShipmentRequest;
import com.orderdesk.web.dto.OrderV2View;
import com.orderdesk.web.dto.ShipmentView;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v2/orders")
public class OrderV2Controller {

    private final ShipmentService shipments;
    private final OrderV2QueryService queries;

    public OrderV2Controller(ShipmentService shipments, OrderV2QueryService queries) {
        this.shipments = shipments;
        this.queries = queries;
    }

    @GetMapping("/{id}")
    public OrderV2View get(@PathVariable long id) {
        return queries.get(id);
    }

    @PostMapping("/{id}/shipments")
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentView ship(@PathVariable long id, @Valid @RequestBody CreateShipmentRequest request) {
        var created = shipments.ship(id, request.lines().stream()
                .map(l -> new ShipmentService.Requested(l.sku(), l.quantity())).toList());
        return queries.shipment(created.getId());
    }
}
