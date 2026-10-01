package com.orderdesk.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "shipment_line")
public class ShipmentLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;
    private int quantity;

    protected ShipmentLine() {
    }

    public ShipmentLine(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }
}
