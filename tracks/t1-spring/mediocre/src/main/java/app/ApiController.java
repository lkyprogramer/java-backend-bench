package app;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
public class ApiController {

    private final InventoryService service;
    private final String token;

    public ApiController(InventoryService service, @Value("${BENCH_TEST_TOKEN:}") String token) {
        this.service = service;
        this.token = token;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/v1/items")
    public synchronized ResponseEntity<?> createItem(@Valid @RequestBody Dtos.CreateItem req) {
        return ResponseEntity.status(201).body(item(service.createItem(req)));
    }

    @GetMapping("/v1/items/{sku}")
    public synchronized Map<String, Object> getItem(@PathVariable String sku) {
        return item(service.getItem(sku));
    }

    @PostMapping("/v1/reservations")
    public synchronized ResponseEntity<?> reserve(@Valid @RequestBody Dtos.CreateReservation req) {
        Object[] result = service.reserve(req);
        return ResponseEntity.status((Boolean) result[1] ? 201 : 200).body(reservation((Entities.ReservationEntity) result[0]));
    }

    @GetMapping("/v1/reservations/{id}")
    public synchronized Map<String, Object> get(@PathVariable long id) {
        return reservation(service.get(id));
    }

    @PostMapping("/v1/reservations/{id}/confirm")
    public synchronized Map<String, Object> confirm(@PathVariable long id, @RequestBody Map<String, Object> body) {
        if (!body.isEmpty()) {
            throw new ApiError(400, "INVALID_REQUEST", "body must be {}");
        }
        return reservation(service.change(id, "CONFIRMED"));
    }

    @PostMapping("/v1/reservations/{id}/cancel")
    public synchronized Map<String, Object> cancel(@PathVariable long id, @RequestBody Map<String, Object> body) {
        if (!body.isEmpty()) {
            throw new ApiError(400, "INVALID_REQUEST", "body must be {}");
        }
        return reservation(service.change(id, "CANCELLED"));
    }

    @GetMapping("/v1/reservations")
    public synchronized Map<String, Object> list(@RequestParam(defaultValue = "0") long after,
                                                 @RequestParam(defaultValue = "20") int limit,
                                                 @RequestParam(required = false) String state) {
        if (after < 0 || limit < 1 || limit > 100) {
            throw new ApiError(400, "INVALID_REQUEST", "bad paging");
        }
        if (state != null && !List.of("ACTIVE", "CONFIRMED", "CANCELLED", "EXPIRED").contains(state)) {
            throw new ApiError(400, "INVALID_REQUEST", "bad state");
        }
        List<Map<String, Object>> page = new ArrayList<>();
        String next = null;
        for (Entities.ReservationEntity r : service.all()) {
            if (r.id <= after || (state != null && !state.equals(r.state))) {
                continue;
            }
            page.add(reservation(r));
            if (page.size() == limit) {
                next = (String) page.get(page.size() - 1).get("id");
                break;
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", page);
        out.put("nextAfter", next);
        return out;
    }

    @GetMapping("/v1/stats")
    public synchronized Map<String, Object> stats() {
        Map<String, Object> out = new LinkedHashMap<>();
        List<Entities.ReservationEntity> all = service.all();
        out.put("items", service.itemCount());
        out.put("reservations", (long) all.size());
        for (String s : List.of("ACTIVE", "CONFIRMED", "CANCELLED", "EXPIRED")) {
            out.put(s.toLowerCase(), all.stream().filter(r -> r.state.equals(s)).count());
        }
        out.put("now", service.now());
        return out;
    }

    @PostMapping("/__test__/advance")
    public synchronized Map<String, Object> advance(@RequestHeader(value = "X-Test-Token", required = false) String header,
                                                    @Valid @RequestBody Dtos.Advance req) {
        if (!service.isManual()) {
            throw new ApiError(404, "NOT_FOUND", "not found");
        }
        if (header == null || !header.equals(token) || token.isEmpty()) {
            throw new ApiError(403, "FORBIDDEN", "bad token");
        }
        return Map.of("now", service.advance(req.millis()));
    }

    private static Map<String, Object> item(Entities.ItemEntity i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sku", i.sku);
        m.put("total", i.total);
        m.put("available", i.available);
        m.put("held", i.held);
        m.put("sold", i.sold);
        return m;
    }

    private static Map<String, Object> reservation(Entities.ReservationEntity r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", String.valueOf(r.id));
        m.put("requestId", r.requestId);
        m.put("state", r.state);
        m.put("createdAt", r.createdAt);
        m.put("expiresAt", r.expiresAt);
        List<Map<String, Object>> lines = new ArrayList<>();
        for (Entities.LineEntity l : r.lines) {
            lines.add(new LinkedHashMap<>(Map.of("sku", l.sku, "quantity", l.quantity)));
        }
        m.put("lines", lines);
        return m;
    }
}
