package com.orderdesk.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "shipment")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "shipment_id", nullable = false)
    @OrderBy("sku")
    private List<ShipmentLine> lines = new ArrayList<>();

    protected Shipment() {
    }

    public Shipment(Long orderId, Instant shippedAt) {
        this.orderId = orderId;
        this.shippedAt = shippedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }

    public List<ShipmentLine> getLines() {
        return lines;
    }
}
