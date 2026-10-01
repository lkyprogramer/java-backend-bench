package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/** Every order shipped through v1 (including ones refunded afterwards) has exactly one complete shipment. */
class HiddenT3BackfillTest extends HiddenT3PristineSupport {

    @Test
    void historicShipmentsAreBackfilled() {
        for (var o : MANIFEST.get("orders")) {
            String label = o.get("label").asText();
            var v1 = o.get("v1");
            var order = v2(o.get("id").asLong());
            var shipments = order.get("shipments");
            if (o.get("expectShipment").asBoolean()) {
                assertThat(shipments).as(label).hasSize(1);
                var shipment = shipments.get(0);
                assertThat(Instant.parse(shipment.get("shippedAt").asText())).as(label).isEqualTo(Instant.parse(v1.get("shippedAt").asText()));
                assertThat(shipment.get("lines")).as(label).hasSize(v1.get("lines").size());
                for (int i = 0; i < v1.get("lines").size(); i++) {
                    var line = v1.get("lines").get(i);
                    assertThat(order.get("lines").get(i).get("shippedQuantity").asInt()).as(label).isEqualTo(line.get("quantity").asInt());
                }
            } else {
                assertThat(shipments).as(label).isEmpty();
                order.get("lines").forEach(l -> assertThat(l.get("shippedQuantity").asInt()).as(label).isZero());
            }
        }
    }
}
