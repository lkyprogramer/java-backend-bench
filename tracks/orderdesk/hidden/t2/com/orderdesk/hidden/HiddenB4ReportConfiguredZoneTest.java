package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** B4: another configured zone, across a daylight-saving change (23-hour business day). */
@TestPropertySource(properties = "orderdesk.business-zone=America/New_York")
class HiddenB4ReportConfiguredZoneTest extends HiddenSupport {

    @Test
    void reportFollowsConfiguredZoneAcrossDstChange() {
        String sku = product(unique("p"), "1.00", 10);
        clock.set(Instant.parse("2026-03-08T05:30:00Z")); // 00:30 EST, 2026-03-08
        order(unique("c"), sku, 1);
        clock.set(Instant.parse("2026-03-09T03:30:00Z")); // 23:30 EDT, 2026-03-08
        order(unique("c"), sku, 2);
        clock.set(Instant.parse("2026-03-09T04:30:00Z")); // 00:30 EDT, 2026-03-09
        order(unique("c"), sku, 4);
        var day = call("GET", "/api/reports/daily?date=2026-03-08", null).body();
        assertThat(day.get("zone").asText()).isEqualTo("America/New_York");
        assertThat(day.get("orderCount").asInt()).isEqualTo(2);
        assertThat(day.get("grossAmount").decimalValue()).isEqualByComparingTo("3.00");
    }
}
