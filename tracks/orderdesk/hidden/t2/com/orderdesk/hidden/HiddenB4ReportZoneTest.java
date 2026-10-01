package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/** B4: the daily report uses the configured business zone (default Asia/Shanghai), never the JVM zone. */
class HiddenB4ReportZoneTest extends HiddenSupport {

    @Test
    void earlyMorningOrderBelongsToTheBusinessDay() {
        String sku = product(unique("p"), "3.00", 10);
        clock.set(Instant.parse("2026-03-01T17:30:00Z")); // 2026-03-02 01:30 in Shanghai
        order(unique("c"), sku, 1);
        clock.set(Instant.parse("2026-03-02T15:59:00Z")); // 2026-03-02 23:59 in Shanghai
        order(unique("c"), sku, 2);
        clock.set(Instant.parse("2026-03-02T16:00:00Z")); // 2026-03-03 00:00 in Shanghai
        order(unique("c"), sku, 4);
        var day = call("GET", "/api/reports/daily?date=2026-03-02", null).body();
        assertThat(day.get("orderCount").asInt()).isEqualTo(2);
        assertThat(day.get("grossAmount").decimalValue()).isEqualByComparingTo("9.00");
        assertThat(call("GET", "/api/reports/daily?date=2026-03-01", null).body().get("orderCount").asInt()).isZero();
    }
}
