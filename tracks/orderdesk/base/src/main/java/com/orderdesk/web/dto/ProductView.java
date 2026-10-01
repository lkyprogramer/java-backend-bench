package com.orderdesk.web.dto;

import java.math.BigDecimal;

import com.orderdesk.domain.Product;
import com.orderdesk.domain.StockLevel;

public record ProductView(String sku, String name, BigDecimal price, int onHand, int reserved, int available) {

    public static ProductView of(Product p, StockLevel s) {
        return new ProductView(p.getSku(), p.getName(), p.getPrice(), s.getOnHand(), s.getReserved(), s.getAvailable());
    }
}
