package contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Black-box contract tests: JDK HttpClient + Jackson only. Every fixture uses fresh SKUs and
 * request keys, so tests are independent of order and of each other.
 */
class ReserveLedgerContractTest {

    private static final String BASE = System.getenv().getOrDefault("BENCH_BASE_URL", "http://127.0.0.1:8080");
    private static final String TOKEN = System.getenv().getOrDefault("BENCH_TEST_TOKEN", "");
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();

    record Reply(int status, JsonNode body) {
    }

    private static Reply call(String method, String path, Object body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + path));
        if (body == null) {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            b.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        }
        if (path.startsWith("/__test__/")) {
            b.header("X-Test-Token", TOKEN);
        }
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(r.statusCode(), JSON.readTree(r.body()));
    }

    private static String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static String item(long quantity) throws Exception {
        return item("s", quantity);
    }

    private static String item(String prefix, long quantity) throws Exception {
        String sku = unique(prefix);
        assertEquals(201, call("POST", "/v1/items", Map.of("sku", sku, "quantity", quantity)).status());
        return sku;
    }

    private static Map<String, Object> reservation(String key, long ttl, Object... skuQuantity) {
        List<Map<String, Object>> lines = new ArrayList<>();
        for (int i = 0; i < skuQuantity.length; i += 2) {
            lines.add(Map.of("sku", skuQuantity[i], "quantity", skuQuantity[i + 1]));
        }
        return Map.of("requestId", key, "lines", lines, "ttlMillis", ttl);
    }

    private static Reply reserve(String key, long ttl, Object... skuQuantity) throws Exception {
        return call("POST", "/v1/reservations", reservation(key, ttl, skuQuantity));
    }

    private static Reply action(JsonNode reservation, String action) throws Exception {
        return call("POST", "/v1/reservations/" + reservation.get("id").asText() + "/" + action, Map.of());
    }

    private static long[] stock(String sku) throws Exception {
        JsonNode s = call("GET", "/v1/items/" + sku, null).body();
        assertEquals(s.get("total").asLong(), s.get("available").asLong() + s.get("held").asLong() + s.get("sold").asLong());
        return new long[] {s.get("available").asLong(), s.get("held").asLong(), s.get("sold").asLong()};
    }

    @Test
    void concurrentReservationsNeverOversell() throws Exception {
        String sku = item(5);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CyclicBarrier start = new CyclicBarrier(16);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            String key = unique("race");
            Callable<Integer> task = () -> {
                start.await();
                return reserve(key, 10_000, sku, 1).status();
            };
            results.add(pool.submit(task));
        }
        int created = 0;
        for (Future<Integer> f : results) {
            created += f.get() == 201 ? 1 : 0;
        }
        pool.shutdown();
        assertEquals(5, created);
        assertEquals(0, stock(sku)[0]);
    }

    @Test
    void sameKeyRetryReturnsOriginalWithoutHoldingAgain() throws Exception {
        String sku = item(10);
        String key = unique("same");
        Reply first = reserve(key, 10_000, sku, 3);
        Reply again = reserve(key, 10_000, sku, 3);
        assertEquals(201, first.status());
        assertEquals(200, again.status());
        assertEquals(first.body().get("id"), again.body().get("id"));
        assertEquals(3, stock(sku)[1]);
    }

    @Test
    void sameKeyDifferentPayloadConflicts() throws Exception {
        String sku = item(10);
        String key = unique("conflict");
        assertEquals(201, reserve(key, 10_000, sku, 1).status());
        Reply other = reserve(key, 10_000, sku, 2);
        assertEquals(409, other.status());
        assertEquals("IDEMPOTENCY_CONFLICT", other.body().path("error").path("code").asText());
    }

    @Test
    void reservationExpiresExactlyAtDeadline() throws Exception {
        String sku = item(2);
        Reply r = reserve(unique("ttl"), 1, sku, 1);
        assertEquals(200, call("POST", "/__test__/advance", Map.of("millis", 1)).status());
        assertEquals("EXPIRED", call("GET", "/v1/reservations/" + r.body().get("id").asText(), null).body().get("state").asText());
        assertEquals(2, stock(sku)[0]);
    }

    @Test
    void repeatedCancelReleasesOnce() throws Exception {
        String sku = item(10);
        JsonNode r = reserve(unique("cancel"), 10_000, sku, 3).body();
        assertEquals(200, action(r, "cancel").status());
        assertEquals(200, action(r, "cancel").status());
        assertEquals(10, stock(sku)[0]);
        assertEquals(0, stock(sku)[1]);
    }

    @Test
    void confirmMovesHeldToSold() throws Exception {
        String sku = item(10);
        JsonNode r = reserve(unique("confirm"), 10_000, sku, 3).body();
        assertEquals("CONFIRMED", action(r, "confirm").body().get("state").asText());
        long[] s = stock(sku);
        assertEquals(7, s[0]);
        assertEquals(0, s[1]);
        assertEquals(3, s[2]);
    }

    @Test
    void cancelledReservationCannotBeConfirmed() throws Exception {
        String sku = item(10);
        JsonNode r = reserve(unique("terminal"), 10_000, sku, 1).body();
        action(r, "cancel");
        Reply confirm = action(r, "confirm");
        assertEquals(409, confirm.status());
        assertEquals("INVALID_STATE", confirm.body().path("error").path("code").asText());
    }

    @Test
    void multiSkuFailureChangesNothing() throws Exception {
        // Lines are processed in sku order: the short line must come second to expose partial holds.
        String a = item("a", 10);
        String b = item("b", 1);
        Reply r = reserve(unique("batch"), 10_000, a, 2, b, 2);
        assertEquals(409, r.status());
        assertEquals("INSUFFICIENT_STOCK", r.body().path("error").path("code").asText());
        assertEquals(10, stock(a)[0]);
        assertEquals(1, stock(b)[0]);
    }

    @Test
    void zeroQuantityLineIsRejected() throws Exception {
        String sku = item(1);
        Reply r = reserve(unique("zero"), 10_000, sku, 0);
        assertEquals(400, r.status());
        assertEquals("INVALID_REQUEST", r.body().path("error").path("code").asText());
    }

    @Test
    void listFiltersByState() throws Exception {
        String sku = item(10);
        reserve(unique("active"), 10_000, sku, 1);
        action(reserve(unique("done"), 10_000, sku, 1).body(), "confirm");
        JsonNode page = call("GET", "/v1/reservations?state=CONFIRMED&limit=100", null).body();
        assertFalse(page.get("items").isEmpty());
        for (JsonNode r : page.get("items")) {
            assertEquals("CONFIRMED", r.get("state").asText());
        }
    }

    @Test
    void errorsUseTheJsonEnvelope() throws Exception {
        Reply r = call("GET", "/v1/items/" + unique("missing"), null);
        assertEquals(404, r.status());
        assertTrue(r.body().path("error").path("message").isTextual());
    }
}
