package com.orderdesk.domain;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product")
public class Product {

    @Id
    private String sku;
    private String name;
    private BigDecimal price;
    private boolean active;

    protected Product() {
    }

    public Product(String sku, String name, BigDecimal price) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.active = true;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }
}
