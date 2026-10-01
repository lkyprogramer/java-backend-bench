package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** Rejected shipments and the rules for orders in PARTIALLY_SHIPPED. */
class HiddenT3ShipmentValidationTest extends HiddenT3MutableSupport {

    @Test
    void invalidShipmentsAreRejectedWithoutSideEffects() {
        String a = product(unique("a"), "1.00", 10);
        String other = product(unique("o"), "1.00", 10);
        long id = paidOrder(a, 2);
        var over = ship(id, a, 3);
        assertThat(over.status()).isEqualTo(422);
        assertThat(over.body().at("/error/code").asText()).isEqualTo("SHIPMENT_INVALID");
        var foreign = ship(id, other, 1);
        assertThat(foreign.status()).isEqualTo(422);
        assertThat(foreign.body().at("/error/code").asText()).isEqualTo("SHIPMENT_INVALID");
        assertThat(call("POST", "/api/v2/orders/" + id + "/shipments", Map.of("lines", java.util.List.of())).status()).isEqualTo(400);
        assertThat(stock(a).get("onHand").asInt()).isEqualTo(10);
        assertThat(v2(id).get("shipments")).isEmpty();
    }

    @Test
    void onlyPaidOrPartiallyShippedOrdersShip() {
        String a = product(unique("a"), "1.00", 10);
        long unpaid = order(unique("c"), a, 1).body().get("id").asLong();
        var r = ship(unpaid, a, 1);
        assertThat(r.status()).isEqualTo(409);
        assertThat(r.body().at("/error/code").asText()).isEqualTo("INVALID_STATE");
        long done = paidOrder(a, 1);
        assertThat(ship(done, a, 1).status()).isEqualTo(201);
        assertThat(ship(done, a, 1).status()).isEqualTo(409);
        assertThat(call("GET", "/api/v2/orders/987654321", null).status()).isEqualTo(404);
    }

    @Test
    void partiallyShippedOrdersCannotBeCancelledOrRefunded() {
        String a = product(unique("a"), "5.00", 10);
        long id = paidOrder(a, 2);
        ship(id, a, 1);
        assertThat(call("POST", "/api/orders/" + id + "/cancel", null).status()).isEqualTo(409);
        assertThat(call("POST", "/api/orders/" + id + "/refunds", Map.of("amount", 5)).status()).isEqualTo(409);
        assertThat(v2(id).get("status").asText()).isEqualTo("PARTIALLY_SHIPPED");
    }
}
