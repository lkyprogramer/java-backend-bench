package com.orderdesk.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

/** Pricing rules shared by order creation and payment. */
@Component
public class OrderPricing {

    /** Orders that cost nothing (e.g. only free samples) are settled immediately. */
    public boolean requiresPayment(BigDecimal total) {
        return !total.equals(BigDecimal.ZERO);
    }
}
