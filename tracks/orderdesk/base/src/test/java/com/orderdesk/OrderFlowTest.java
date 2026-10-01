package com.orderdesk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.orderdesk.support.ApiTestSupport;

class OrderFlowTest extends ApiTestSupport {

    @Test
    void placeReservesStockAndComputesTotal() {
        String sku = product(unique("p"), "12.50", 10);
        var r = order(unique("c"), sku, 2);
        assertThat(r.status()).isEqualTo(201);
        assertThat(r.body().get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(r.body().get("total").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(stock(sku).get("reserved").asInt()).isEqualTo(2);
    }

    @Test
    void insufficientStockIsRejected() {
        String sku = product(unique("p"), "1.00", 1);
        var r = order(unique("c"), sku, 2);
        assertThat(r.status()).isEqualTo(409);
        assertThat(r.body().at("/error/code").asText()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(stock(sku).get("reserved").asInt()).isZero();
    }

    @Test
    void payThenShipMovesStockOut() {
        String sku = product(unique("p"), "5.00", 4);
        long id = order(unique("c"), sku, 3).body().get("id").asLong();
        var paid = pay(id, GOOD_CARD);
        assertThat(paid.status()).isEqualTo(200);
        assertThat(paid.body().get("status").asText()).isEqualTo("PAID");
        var shipped = call("POST", "/api/orders/" + id + "/ship", null);
        assertThat(shipped.body().get("status").asText()).isEqualTo("SHIPPED");
        var s = stock(sku);
        assertThat(s.get("onHand").asInt()).isEqualTo(1);
        assertThat(s.get("reserved").asInt()).isZero();
    }

    @Test
    void cancelReleasesReservation() {
        String sku = product(unique("p"), "5.00", 4);
        long id = order(unique("c"), sku, 3).body().get("id").asLong();
        var r = call("POST", "/api/orders/" + id + "/cancel", null);
        assertThat(r.body().get("status").asText()).isEqualTo("CANCELLED");
        assertThat(stock(sku).get("available").asInt()).isEqualTo(4);
    }

    @Test
    void partialRefundKeepsOrderPartiallyRefunded() {
        String sku = product(unique("p"), "40.00", 4);
        long id = order(unique("c"), sku, 1).body().get("id").asLong();
        pay(id, GOOD_CARD);
        var r = call("POST", "/api/orders/" + id + "/refunds", Map.of("amount", 15));
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body().get("status").asText()).isEqualTo("PARTIALLY_REFUNDED");
        assertThat(r.body().get("refundedAmount").decimalValue()).isEqualByComparingTo("15");
    }

    @Test
    void listsOrdersOfCustomer() {
        String sku = product(unique("p"), "2.00", 10);
        String customer = unique("c");
        order(customer, sku, 1);
        order(customer, sku, 2);
        var r = call("GET", "/api/orders?customerId=" + customer, null);
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body()).hasSize(2);
        assertThat(r.body().get(1).get("lineCount").asInt()).isEqualTo(1);
    }
}
