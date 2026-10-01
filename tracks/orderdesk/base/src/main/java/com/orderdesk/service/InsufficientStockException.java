package com.orderdesk.service;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String sku, int requested, int available) {
        super(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                "Insufficient stock for " + sku + ": requested " + requested + ", available " + available);
    }
}
