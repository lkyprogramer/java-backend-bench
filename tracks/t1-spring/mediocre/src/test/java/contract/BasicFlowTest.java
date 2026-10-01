package contract;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class BasicFlowTest {

    static final String BASE = System.getenv().getOrDefault("BENCH_BASE_URL", "http://127.0.0.1:8080");
    static final HttpClient CLIENT = HttpClient.newHttpClient();
    static final ObjectMapper MAPPER = new ObjectMapper();

    static HttpResponse<String> post(String path, String json) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(BASE + path)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }

    static JsonNode get(String path) throws Exception {
        return MAPPER.readTree(CLIENT.send(HttpRequest.newBuilder(URI.create(BASE + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body());
    }

    @Test
    void createAndGetItem() throws Exception {
        assertEquals(201, post("/v1/items", "{\"sku\":\"t1\",\"quantity\":5}").statusCode());
        assertEquals(5, get("/v1/items/t1").get("available").asLong());
    }

    @Test
    void reserveAndConfirm() throws Exception {
        post("/v1/items", "{\"sku\":\"t2\",\"quantity\":5}");
        JsonNode r = MAPPER.readTree(post("/v1/reservations", "{\"requestId\":\"k2\",\"lines\":[{\"sku\":\"t2\",\"quantity\":2}],\"ttlMillis\":1000}").body());
        post("/v1/reservations/" + r.get("id").asText() + "/confirm", "{}");
        JsonNode item = get("/v1/items/t2");
        assertEquals(3, item.get("available").asLong());
        assertEquals(2, item.get("sold").asLong());
    }

    @Test
    void reserveAndCancel() throws Exception {
        post("/v1/items", "{\"sku\":\"t3\",\"quantity\":5}");
        JsonNode r = MAPPER.readTree(post("/v1/reservations", "{\"requestId\":\"k3\",\"lines\":[{\"sku\":\"t3\",\"quantity\":2}],\"ttlMillis\":1000}").body());
        post("/v1/reservations/" + r.get("id").asText() + "/cancel", "{}");
        assertEquals(5, get("/v1/items/t3").get("available").asLong());
    }

    @Test
    void retryReturnsSameReservation() throws Exception {
        post("/v1/items", "{\"sku\":\"t4\",\"quantity\":5}");
        String body = "{\"requestId\":\"k4\",\"lines\":[{\"sku\":\"t4\",\"quantity\":1}],\"ttlMillis\":1000}";
        JsonNode a = MAPPER.readTree(post("/v1/reservations", body).body());
        HttpResponse<String> again = post("/v1/reservations", body);
        assertEquals(200, again.statusCode());
        assertEquals(a.get("id").asText(), MAPPER.readTree(again.body()).get("id").asText());
    }
}
