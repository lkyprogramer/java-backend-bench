package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** v1 responses for existing data are byte-for-byte the same JSON as before the release. */
class HiddenT3V1ReadCompatTest extends HiddenT3PristineSupport {

    @Test
    void v1OrderResponsesUnchanged() {
        for (var o : MANIFEST.get("orders")) {
            assertThat(call("GET", "/api/orders/" + o.get("id").asLong(), null).body()).as(o.get("label").asText()).isEqualTo(o.get("v1"));
        }
    }

    @Test
    void v1ListUnchanged() {
        assertThat(call("GET", "/api/orders?customerId=seed-customer", null).body()).isEqualTo(MANIFEST.get("v1List"));
    }
}
