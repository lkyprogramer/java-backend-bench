package com.orderdesk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.orderdesk.support.ApiTestSupport;

class ReportApiTest extends ApiTestSupport {

    @Test
    void dailyReportCountsOrdersOfTheDay() {
        String sku = product(unique("p"), "3.00", 10);
        clock.set(Instant.parse("2026-02-10T06:00:00Z"));
        order(unique("c"), sku, 1);
        order(unique("c"), sku, 2);
        var r = call("GET", "/api/reports/daily?date=2026-02-10", null);
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body().get("orderCount").asInt()).isEqualTo(2);
        assertThat(r.body().get("grossAmount").decimalValue()).isEqualByComparingTo("9.00");
    }
}
