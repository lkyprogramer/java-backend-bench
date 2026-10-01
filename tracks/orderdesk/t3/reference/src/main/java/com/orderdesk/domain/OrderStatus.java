package com.orderdesk.domain;

public enum OrderStatus {
    PENDING_PAYMENT, PAID, PARTIALLY_SHIPPED, SHIPPED, CANCELLED, PARTIALLY_REFUNDED, REFUNDED;

    /** Status as published by the v1 API, which predates partial shipments. */
    public String v1Name() {
        return this == PARTIALLY_SHIPPED ? PAID.name() : name();
    }
}
