package com.orderdesk.web.dto;

import com.orderdesk.domain.StockLevel;

public record StockView(String sku, int onHand, int reserved, int available) {

    public static StockView of(StockLevel s) {
        return new StockView(s.getSku(), s.getOnHand(), s.getReserved(), s.getAvailable());
    }
}
