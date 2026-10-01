package contract;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

/** Example black-box contract test. The judge sets BENCH_BASE_URL and BENCH_TEST_TOKEN. */
class HealthContractTest {

    private static final String BASE = System.getenv().getOrDefault("BENCH_BASE_URL", "http://127.0.0.1:8080");

    @Test
    void healthReportsOk() throws Exception {
        HttpResponse<String> r = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(BASE + "/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, r.statusCode());
    }
}
