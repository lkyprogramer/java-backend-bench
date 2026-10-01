package app.web;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import app.service.ApiException;
import app.service.Ledger;
import app.service.LedgerClock;
import jakarta.servlet.http.HttpServletRequest;

/** Exists only in manual clock mode; in system mode the route is reported as unknown for every method. */
@RestController
public class TestClockController {

    private final Ledger ledger;
    private final LedgerClock clock;
    private final Requests requests;
    private final String token;

    public TestClockController(Ledger ledger, LedgerClock clock, Requests requests,
            @Value("${BENCH_TEST_TOKEN:}") String token) {
        this.ledger = ledger;
        this.clock = clock;
        this.requests = requests;
        this.token = token;
    }

    @RequestMapping("/__test__/advance")
    public Map<String, Object> advance(HttpServletRequest request) throws IOException {
        if (!clock.manual()) {
            throw ApiException.notFound("Unknown route");
        }
        if (!"POST".equals(request.getMethod())) {
            throw new ApiException(405, "METHOD_NOT_ALLOWED", "Method not allowed");
        }
        if (token.isEmpty() || !token.equals(request.getHeader("X-Test-Token"))) {
            throw new ApiException(403, "FORBIDDEN", "Test token required");
        }
        Requests.noQuery(request);
        JsonNode body = requests.object(request);
        Requests.fields(body, "millis");
        return Map.of("now", ledger.advance(Requests.integer(body.get("millis"), 0, 86_400_000)));
    }
}
