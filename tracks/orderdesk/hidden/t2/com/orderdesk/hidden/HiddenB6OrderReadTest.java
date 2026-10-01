package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/** B6: order details load without open-in-view or lazy-loading hacks, and without N+1 queries. */
class HiddenB6OrderReadTest extends HiddenSupport {

    @Autowired
    private Environment environment;

    @Test
    void orderDetailIncludesLines() {
        String a = product(unique("a"), "2.00", 5);
        String b = product(unique("b"), "3.00", 5);
        long id = order(unique("c"), a, 1, b, 2).body().get("id").asLong();
        var r = call("GET", "/api/orders/" + id, null);
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body().get("lines")).hasSize(2);
        assertThat(r.body().get("total").decimalValue()).isEqualByComparingTo("8.00");
    }

    @Test
    void projectConventionsStayInPlace() {
        assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
        assertThat(environment.getProperty("spring.jpa.properties.hibernate.enable_lazy_load_no_trans", "false")).isEqualTo("false");
    }

    @Test
    void readsDoNotIssueQueryPerOrder() {
        String sku = product(unique("p"), "1.00", 100);
        String customer = unique("c");
        clock.set(Instant.parse("2026-04-20T03:00:00Z"));
        long last = 0;
        for (int i = 0; i < 12; i++) {
            last = order(customer, sku, 1).body().get("id").asLong();
        }
        long id = last;
        assertThat(statements(() -> call("GET", "/api/orders/" + id, null))).isLessThanOrEqualTo(3);
        assertThat(statements(() -> call("GET", "/api/orders?customerId=" + customer, null))).isLessThanOrEqualTo(3);
        assertThat(statements(() -> call("GET", "/api/reports/daily?date=2026-04-20", null))).isLessThanOrEqualTo(3);
    }
}
