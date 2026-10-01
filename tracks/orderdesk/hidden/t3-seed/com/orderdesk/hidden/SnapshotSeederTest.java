package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Judge-only: builds the "production" v1 database that track t3 migrates. Runs against the v1.1 codebase
 * (all T2 fixes, no shipments) with a file database in BENCH_SNAPSHOT_DIR, and records every order's v1
 * response so that compatibility can be checked after the migration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:file:${BENCH_SNAPSHOT_DIR}/orderdesk;LOCK_TIMEOUT=10000")
@Import(SnapshotSeederTest.ClockConfig.class)
class SnapshotSeederTest {

    static final ObjectMapper JSON = new ObjectMapper();
    static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    int port;

    @Autowired
    SeedClock clock;

    JsonNode call(String method, String path, Object body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (body == null) {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            b.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        }
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).as(method + " " + path + " " + r.body()).isLessThan(300);
        return JSON.readTree(r.body());
    }

    long order(Object... skuQuantity) throws Exception {
        List<Map<String, Object>> lines = new ArrayList<>();
        for (int i = 0; i < skuQuantity.length; i += 2) {
            lines.add(Map.of("sku", skuQuantity[i], "quantity", skuQuantity[i + 1]));
        }
        return call("POST", "/api/orders", Map.of("customerId", "seed-customer", "lines", lines)).get("id").asLong();
    }

    void pay(long id) throws Exception {
        call("POST", "/api/orders/" + id + "/pay", Map.of("cardNumber", "4242424242424242"));
    }

    @Test
    void seed() throws Exception {
        call("POST", "/api/products", Map.of("sku", "SEED-A", "name", "Seed A", "price", new BigDecimal("10.00"), "initialStock", 500));
        call("POST", "/api/products", Map.of("sku", "SEED-B", "name", "Seed B", "price", new BigDecimal("2.50"), "initialStock", 500));
        call("POST", "/api/products", Map.of("sku", "SEED-FREE", "name", "Sample", "price", new BigDecimal("0.00"), "initialStock", 500));
        Map<String, Long> ids = new LinkedHashMap<>();
        Map<String, Boolean> shipped = new LinkedHashMap<>();
        clock.set(Instant.parse("2025-11-03T02:00:00Z"));
        ids.put("pending", order("SEED-A", 1));
        shipped.put("pending", false);
        clock.set(Instant.parse("2025-11-03T03:00:00Z"));
        ids.put("paid", order("SEED-A", 2, "SEED-B", 1));
        pay(ids.get("paid"));
        shipped.put("paid", false);
        clock.set(Instant.parse("2025-11-04T05:00:00Z"));
        ids.put("shipped", order("SEED-A", 1, "SEED-B", 4));
        pay(ids.get("shipped"));
        clock.set(Instant.parse("2025-11-05T06:30:00Z"));
        call("POST", "/api/orders/" + ids.get("shipped") + "/ship", null);
        shipped.put("shipped", true);
        clock.set(Instant.parse("2025-11-06T07:00:00Z"));
        ids.put("shippedPartlyRefunded", order("SEED-B", 2));
        pay(ids.get("shippedPartlyRefunded"));
        clock.set(Instant.parse("2025-11-06T09:15:00Z"));
        call("POST", "/api/orders/" + ids.get("shippedPartlyRefunded") + "/ship", null);
        call("POST", "/api/orders/" + ids.get("shippedPartlyRefunded") + "/refunds", Map.of("amount", 1));
        shipped.put("shippedPartlyRefunded", true);
        clock.set(Instant.parse("2025-11-07T01:00:00Z"));
        ids.put("shippedRefunded", order("SEED-A", 3));
        pay(ids.get("shippedRefunded"));
        clock.set(Instant.parse("2025-11-07T02:45:00Z"));
        call("POST", "/api/orders/" + ids.get("shippedRefunded") + "/ship", null);
        call("POST", "/api/orders/" + ids.get("shippedRefunded") + "/refunds", Map.of("amount", 30));
        shipped.put("shippedRefunded", true);
        clock.set(Instant.parse("2025-11-08T04:00:00Z"));
        ids.put("cancelled", order("SEED-B", 1));
        call("POST", "/api/orders/" + ids.get("cancelled") + "/cancel", null);
        shipped.put("cancelled", false);
        clock.set(Instant.parse("2025-11-09T04:00:00Z"));
        ids.put("refundedUnshipped", order("SEED-A", 1));
        pay(ids.get("refundedUnshipped"));
        call("POST", "/api/orders/" + ids.get("refundedUnshipped") + "/refunds", Map.of("amount", 10));
        shipped.put("refundedUnshipped", false);
        clock.set(Instant.parse("2025-11-10T08:00:00Z"));
        ids.put("freeShipped", order("SEED-FREE", 2));
        clock.set(Instant.parse("2025-11-10T08:30:00Z"));
        call("POST", "/api/orders/" + ids.get("freeShipped") + "/ship", null);
        shipped.put("freeShipped", true);

        Map<String, Object> manifest = new LinkedHashMap<>();
        List<Map<String, Object>> orders = new ArrayList<>();
        for (var e : ids.entrySet()) {
            JsonNode v1 = call("GET", "/api/orders/" + e.getValue(), null);
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("label", e.getKey());
            o.put("id", e.getValue());
            o.put("expectShipment", shipped.get(e.getKey()));
            o.put("v1", v1);
            orders.add(o);
        }
        manifest.put("orders", orders);
        manifest.put("v1List", call("GET", "/api/orders?customerId=seed-customer", null));
        manifest.put("stock", Map.of("SEED-A", call("GET", "/api/products/SEED-A", null), "SEED-B", call("GET", "/api/products/SEED-B", null)));
        Files.writeString(Path.of(System.getenv("BENCH_SNAPSHOT_DIR"), "manifest.json"), JSON.writerWithDefaultPrettyPrinter().writeValueAsString(manifest));
    }

    public static final class SeedClock extends Clock {
        private volatile Instant now = Instant.parse("2025-11-01T00:00:00Z");

        void set(Instant instant) {
            now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        SeedClock seedClock() {
            return new SeedClock();
        }
    }
}
