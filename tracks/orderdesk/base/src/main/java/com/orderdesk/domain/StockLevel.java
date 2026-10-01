package com.orderdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_level")
public class StockLevel {

    @Id
    private String sku;

    @Column(name = "on_hand")
    private int onHand;

    private int reserved;

    protected StockLevel() {
    }

    public StockLevel(String sku, int onHand) {
        this.sku = sku;
        this.onHand = onHand;
    }

    public String getSku() {
        return sku;
    }

    public int getOnHand() {
        return onHand;
    }

    public int getReserved() {
        return reserved;
    }

    public int getAvailable() {
        return onHand - reserved;
    }

    public void setOnHand(int onHand) {
        this.onHand = onHand;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }
}
