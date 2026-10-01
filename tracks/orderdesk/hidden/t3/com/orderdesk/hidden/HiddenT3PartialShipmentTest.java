package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/** v2 partial shipments: status, stock movement and completion. */
class HiddenT3PartialShipmentTest extends HiddenT3MutableSupport {

    @Test
    void shipmentsMoveStockAndCompleteTheOrder() {
        String a = product(unique("a"), "1.00", 10);
        String b = product(unique("b"), "2.00", 10);
        long id = paidOrder(a, 3, b, 2);
        clock.set(Instant.parse("2026-06-01T01:00:00Z"));
        var first = ship(id, a, 1);
        assertThat(first.status()).isEqualTo(201);
        assertThat(first.body().get("orderId").asLong()).isEqualTo(id);
        assertThat(first.body().get("lines")).hasSize(1);
        assertThat(v2(id).get("status").asText()).isEqualTo("PARTIALLY_SHIPPED");
        assertThat(stock(a).get("onHand").asInt()).isEqualTo(9);
        assertThat(stock(a).get("reserved").asInt()).isEqualTo(2);
        clock.set(Instant.parse("2026-06-02T01:00:00Z"));
        assertThat(ship(id, b, 2, a, 2).status()).isEqualTo(201);
        var order = v2(id);
        assertThat(order.get("status").asText()).isEqualTo("SHIPPED");
        assertThat(Instant.parse(order.get("shippedAt").asText())).isEqualTo(Instant.parse("2026-06-02T01:00:00Z"));
        assertThat(stock(b).get("onHand").asInt()).isEqualTo(8);
        assertThat(stock(b).get("reserved").asInt()).isZero();
    }
}
