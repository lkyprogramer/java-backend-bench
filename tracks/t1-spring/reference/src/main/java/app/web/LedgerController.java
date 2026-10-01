package app.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import app.domain.Line;
import app.domain.State;
import app.service.ApiException;
import app.service.Ledger;
import app.service.Mutant;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class LedgerController {

    private static final Set<String> LIST_PARAMS = Set.of("after", "limit", "state");

    private final Ledger ledger;
    private final Requests requests;

    public LedgerController(Ledger ledger, Requests requests) {
        this.ledger = ledger;
        this.requests = requests;
    }

    @GetMapping("/health")
    public Map<String, String> health(HttpServletRequest request) {
        Requests.noQuery(request);
        return Map.of("status", "ok");
    }

    @PostMapping("/v1/items")
    public ResponseEntity<Map<String, Object>> createItem(HttpServletRequest request) throws IOException {
        Requests.noQuery(request);
        JsonNode body = requests.object(request);
        Requests.fields(body, "sku", "quantity");
        String sku = Requests.sku(body.get("sku"));
        long quantity = Requests.integer(body.get("quantity"), 0, 1_000_000_000);
        return ResponseEntity.status(201).body(ledger.createItem(sku, quantity));
    }

    @GetMapping("/v1/items/{sku}")
    public Map<String, Object> item(@PathVariable String sku, HttpServletRequest request) {
        Requests.noQuery(request);
        return ledger.item(Requests.sku(sku));
    }

    @PostMapping("/v1/reservations")
    public ResponseEntity<Map<String, Object>> reserve(HttpServletRequest request) throws IOException {
        Requests.noQuery(request);
        JsonNode body = requests.object(request);
        Requests.fields(body, "requestId", "lines", "ttlMillis");
        String requestId = Requests.requestId(body.get("requestId"));
        long ttl = Requests.integer(body.get("ttlMillis"), 1, 86_400_000);
        JsonNode raw = body.get("lines");
        if (!raw.isArray() || raw.isEmpty() || raw.size() > 20) {
            throw ApiException.invalid("lines must be an array of 1..20 objects");
        }
        List<Line> lines = new ArrayList<>();
        for (JsonNode line : raw) {
            if (!line.isObject()) {
                throw ApiException.invalid("line must be an object");
            }
            Requests.fields(line, "sku", "quantity");
            lines.add(new Line(Requests.sku(line.get("sku")),
                    Requests.integer(line.get("quantity"), Mutant.M09.on() ? 0 : 1, 1_000_000_000)));
        }
        Ledger.Created created = ledger.reserve(requestId, lines, ttl);
        return ResponseEntity.status(created.created() ? 201 : 200).body(created.view());
    }

    @GetMapping("/v1/reservations")
    public Map<String, Object> list(HttpServletRequest request) {
        Map<String, String> q = Requests.query(request, LIST_PARAMS);
        long after = Requests.decimal(q.getOrDefault("after", "0"), true);
        long limit = Requests.decimal(q.getOrDefault("limit", "20"), false);
        if (limit > 100) {
            throw ApiException.invalid("limit must be 1..100");
        }
        State state = null;
        if (q.containsKey("state")) {
            try {
                state = State.valueOf(q.get("state"));
            } catch (IllegalArgumentException e) {
                throw ApiException.invalid("Unknown state");
            }
        }
        return ledger.list(after, (int) limit, state);
    }

    @GetMapping("/v1/reservations/{id}")
    public Map<String, Object> reservation(@PathVariable String id, HttpServletRequest request) {
        Requests.noQuery(request);
        return ledger.reservation(Requests.decimal(id, false));
    }

    @PostMapping("/v1/reservations/{id}/confirm")
    public Map<String, Object> confirm(@PathVariable String id, HttpServletRequest request) throws IOException {
        return transition(id, request, State.CONFIRMED);
    }

    @PostMapping("/v1/reservations/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable String id, HttpServletRequest request) throws IOException {
        return transition(id, request, State.CANCELLED);
    }

    private Map<String, Object> transition(String id, HttpServletRequest request, State target) throws IOException {
        Requests.noQuery(request);
        Requests.fields(requests.object(request));
        return ledger.transition(Requests.decimal(id, false), target);
    }

    @GetMapping("/v1/stats")
    public Map<String, Object> stats(HttpServletRequest request) {
        Requests.noQuery(request);
        return ledger.stats();
    }
}
