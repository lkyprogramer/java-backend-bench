package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/** v1 clients never see PARTIALLY_SHIPPED, and v1 ship still ships everything that is left. */
class HiddenT3V1ShipCompatTest extends HiddenT3MutableSupport {

    @Test
    void partiallyShippedOrderLooksPaidToV1AndV1ShipCompletesIt() {
        String a = product(unique("a"), "3.00", 10);
        String b = product(unique("b"), "4.00", 10);
        long id = paidOrder(a, 3, b, 1);
        assertThat(ship(id, a, 1).status()).isEqualTo(201);
        var v1 = call("GET", "/api/orders/" + id, null).body();
        assertThat(v1.get("status").asText()).isEqualTo("PAID");
        assertThat(v1.get("shippedAt").isNull()).isTrue();
        clock.set(Instant.parse("2026-05-01T10:00:00Z"));
        var shipped = call("POST", "/api/orders/" + id + "/ship", null);
        assertThat(shipped.status()).isEqualTo(200);
        assertThat(shipped.body().get("status").asText()).isEqualTo("SHIPPED");
        assertThat(Instant.parse(shipped.body().get("shippedAt").asText())).isEqualTo(Instant.parse("2026-05-01T10:00:00Z"));
        assertThat(stock(a).get("onHand").asInt()).isEqualTo(7);
        assertThat(stock(a).get("reserved").asInt()).isZero();
        assertThat(v2(id).get("shipments")).hasSize(2);
    }

    @Test
    void seededPaidOrderShipsThroughV1() {
        long id = seeded("paid").get("id").asLong();
        var r = call("POST", "/api/orders/" + id + "/ship", null);
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body().get("status").asText()).isEqualTo("SHIPPED");
        var order = v2(id);
        assertThat(order.get("shipments")).hasSize(1);
        order.get("lines").forEach(l -> assertThat(l.get("shippedQuantity").asInt()).isEqualTo(l.get("quantity").asInt()));
    }
}
