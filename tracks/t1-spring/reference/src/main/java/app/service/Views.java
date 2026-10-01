package app.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.domain.Item;
import app.domain.Line;
import app.domain.Reservation;

/** Response bodies; field order follows the contract examples. */
final class Views {

    private Views() {
    }

    static Map<String, Object> item(Item i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sku", i.getSku());
        m.put("total", i.getTotal());
        m.put("available", i.getAvailable());
        m.put("held", i.getHeld());
        m.put("sold", i.getSold());
        return m;
    }

    static Map<String, Object> reservation(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", Long.toString(r.getId()));
        m.put("requestId", r.getRequestId());
        m.put("state", r.getState().name());
        m.put("createdAt", r.getCreatedAt());
        m.put("expiresAt", r.getExpiresAt());
        m.put("lines", lines(r.getLines()));
        return m;
    }

    private static List<Map<String, Object>> lines(List<Line> lines) {
        return lines.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sku", l.sku());
            m.put("quantity", l.quantity());
            return m;
        }).toList();
    }
}
