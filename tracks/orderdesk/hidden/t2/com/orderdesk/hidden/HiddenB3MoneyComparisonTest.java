package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** B3: money is compared by value, not by BigDecimal scale, everywhere it decides a state. */
class HiddenB3MoneyComparisonTest extends HiddenSupport {

    @Test
    void fullRefundInOneStepMarksRefunded() {
        String sku = product(unique("p"), "40.00", 5);
        long id = order(unique("c"), sku, 1).body().get("id").asLong();
        pay(id, GOOD_CARD);
        var r = call("POST", "/api/orders/" + id + "/refunds", Map.of("amount", 40));
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body().get("status").asText()).isEqualTo("REFUNDED");
    }

    @Test
    void partialRefundsAddingUpToPaymentMarkRefunded() {
        String sku = product(unique("p"), "40.00", 5);
        long id = order(unique("c"), sku, 1).body().get("id").asLong();
        pay(id, GOOD_CARD);
        call("POST", "/api/orders/" + id + "/refunds", Map.of("amount", new java.math.BigDecimal("15.5")));
        var r = call("POST", "/api/orders/" + id + "/refunds", Map.of("amount", new java.math.BigDecimal("24.50")));
        assertThat(r.body().get("status").asText()).isEqualTo("REFUNDED");
    }

    @Test
    void freeOrderIsSettledImmediatelyAndShippable() {
        String gift = product(unique("gift"), "0.00", 5);
        var r = order(unique("c"), gift, 2);
        assertThat(r.status()).isEqualTo(201);
        assertThat(r.body().get("status").asText()).isEqualTo("PAID");
        var shipped = call("POST", "/api/orders/" + r.body().get("id").asLong() + "/ship", null);
        assertThat(shipped.status()).isEqualTo(200);
        assertThat(shipped.body().get("status").asText()).isEqualTo("SHIPPED");
    }
}
