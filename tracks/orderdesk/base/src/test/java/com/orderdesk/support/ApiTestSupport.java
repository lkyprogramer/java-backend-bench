package com.orderdesk.support;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Black-box HTTP helpers against the application started on a random port with an in-memory database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:${random.uuid};DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@Import(ApiTestSupport.ClockConfig.class)
public abstract class ApiTestSupport {

    protected static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    protected int port;

    @Autowired
    protected MutableClock clock;

    public record Reply(int status, JsonNode body) {
    }

    protected Reply call(String method, String path, Object body) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
            if (body == null) {
                b.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                b.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
            }
            HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return new Reply(r.statusCode(), r.body().isEmpty() ? null : JSON.readTree(r.body()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected String product(String sku, String price, int stock) {
        Reply r = call("POST", "/api/products", Map.of("sku", sku, "name", "Item " + sku, "price", new BigDecimal(price), "initialStock", stock));
        if (r.status() != 201) {
            throw new IllegalStateException("product setup failed: " + r);
        }
        return sku;
    }

    protected JsonNode stock(String sku) {
        return call("GET", "/api/products/" + sku, null).body();
    }

    protected Reply order(String customer, Object... skuQuantity) {
        var lines = new java.util.ArrayList<Map<String, Object>>();
        for (int i = 0; i < skuQuantity.length; i += 2) {
            lines.add(Map.of("sku", skuQuantity[i], "quantity", skuQuantity[i + 1]));
        }
        return call("POST", "/api/orders", Map.of("customerId", customer, "lines", lines));
    }

    protected Reply pay(long orderId, String card) {
        return call("POST", "/api/orders/" + orderId + "/pay", Map.of("cardNumber", card));
    }

    protected static final String GOOD_CARD = "4242424242424242";
    protected static final String DECLINED_CARD = "4000000000000002";

    /** Clock the tests can move; registered as the primary Clock bean. */
    public static final class MutableClock extends Clock {
        private volatile Instant now = Instant.parse("2026-01-15T04:00:00Z");

        public void set(Instant instant) {
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
    public static class ClockConfig {
        @Bean
        @Primary
        public MutableClock testClock() {
            return new MutableClock();
        }
    }
}
