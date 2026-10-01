package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** v2 order read: shipments in creation order with lines, per-line shipped quantity, no N+1. */
class HiddenT3V2ReadTest extends HiddenT3MutableSupport {

    @Test
    void v2OrderShowsShipmentsAndShippedQuantities() {
        String a = product(unique("a"), "1.00", 20);
        String b = product(unique("b"), "1.00", 20);
        long id = paidOrder(a, 5, b, 3);
        ship(id, a, 1);
        ship(id, a, 2, b, 1);
        ship(id, b, 1);
        var order = v2(id);
        assertThat(order.get("shipments")).hasSize(3);
        assertThat(order.get("shipments").get(1).get("lines")).hasSize(2);
        assertThat(order.get("shipments").get(0).get("id").asLong()).isLessThan(order.get("shipments").get(2).get("id").asLong());
        assertThat(order.get("lines").get(0).get("sku").asText()).isEqualTo(a);
        assertThat(order.get("lines").get(0).get("shippedQuantity").asInt()).isEqualTo(3);
        assertThat(order.get("lines").get(1).get("shippedQuantity").asInt()).isEqualTo(2);
        assertThat(order.get("total").decimalValue()).isEqualByComparingTo("8.00");
        assertThat(statements(() -> call("GET", "/api/v2/orders/" + id, null))).isLessThanOrEqualTo(4);
    }
}
