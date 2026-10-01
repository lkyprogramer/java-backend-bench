package app;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final ItemRepo items;
    private final ReservationRepo reservations;
    private final SettingRepo settings;
    private final boolean manual;
    private final long initialNow;

    public InventoryService(ItemRepo items, ReservationRepo reservations, SettingRepo settings,
                            @Value("${bench.clock:system}") String clock, @Value("${bench.now:1700000000000}") long now) {
        this.items = items;
        this.reservations = reservations;
        this.settings = settings;
        this.manual = "manual".equals(clock);
        this.initialNow = now;
    }

    public boolean isManual() {
        return manual;
    }

    public long now() {
        if (!manual) {
            return System.currentTimeMillis();
        }
        return settings.findById("clock").map(s -> s.val).orElse(initialNow);
    }

    @Transactional
    public long advance(long millis) {
        long next = now() + millis;
        if (next > 8000000000000000L) {
            throw new ApiError(400, "INVALID_REQUEST", "too far");
        }
        Entities.Setting s = settings.findById("clock").orElseGet(() -> {
            Entities.Setting n = new Entities.Setting();
            n.name = "clock";
            return n;
        });
        s.val = next;
        settings.save(s);
        return next;
    }

    private void expire() {
        for (Entities.ReservationEntity r : reservations.findByStateAndExpiresAtLessThanEqual("ACTIVE", now())) {
            for (Entities.LineEntity l : r.lines) {
                Entities.ItemEntity item = items.findById(l.sku).get();
                item.held -= l.quantity;
                item.available += l.quantity;
            }
            r.state = "EXPIRED";
        }
    }

    @Transactional
    public Entities.ItemEntity createItem(Dtos.CreateItem req) {
        expire();
        if (items.existsById(req.sku())) {
            throw new ApiError(409, "ITEM_EXISTS", "exists");
        }
        Entities.ItemEntity item = new Entities.ItemEntity();
        item.sku = req.sku();
        item.total = req.quantity();
        item.available = req.quantity();
        item.held = 0L;
        item.sold = 0L;
        return items.save(item);
    }

    @Transactional
    public Entities.ItemEntity getItem(String sku) {
        expire();
        return items.findById(sku).orElseThrow(() -> new ApiError(404, "NOT_FOUND", "no item"));
    }

    @Transactional
    public Object[] reserve(Dtos.CreateReservation req) {
        List<Dtos.LineDto> lines = new ArrayList<>(req.lines());
        lines.sort(Comparator.comparing(Dtos.LineDto::sku));
        Set<String> skus = new HashSet<>();
        for (Dtos.LineDto l : lines) {
            if (!skus.add(l.sku())) {
                throw new ApiError(400, "INVALID_REQUEST", "duplicate sku");
            }
        }
        Optional<Entities.ReservationEntity> existing = reservations.findByRequestId(req.requestId());
        if (existing.isPresent()) {
            Entities.ReservationEntity r = existing.get();
            boolean same = r.ttlMillis.equals(req.ttlMillis()) && r.lines.size() == lines.size();
            for (int i = 0; same && i < lines.size(); i++) {
                same = r.lines.get(i).sku.equals(lines.get(i).sku()) && r.lines.get(i).quantity.equals(lines.get(i).quantity());
            }
            if (!same) {
                throw new ApiError(409, "IDEMPOTENCY_CONFLICT", "conflict");
            }
            return new Object[] {r, false};
        }
        List<Entities.ItemEntity> found = new ArrayList<>();
        for (Dtos.LineDto l : lines) {
            found.add(items.findById(l.sku()).orElseThrow(() -> new ApiError(404, "NOT_FOUND", "no item")));
        }
        for (int i = 0; i < lines.size(); i++) {
            if (found.get(i).available < lines.get(i).quantity()) {
                throw new ApiError(409, "INSUFFICIENT_STOCK", "not enough");
            }
        }
        Entities.ReservationEntity r = new Entities.ReservationEntity();
        r.requestId = req.requestId();
        r.state = "ACTIVE";
        r.createdAt = now();
        r.expiresAt = r.createdAt + req.ttlMillis();
        r.ttlMillis = req.ttlMillis();
        for (int i = 0; i < lines.size(); i++) {
            found.get(i).available -= lines.get(i).quantity();
            found.get(i).held += lines.get(i).quantity();
            Entities.LineEntity le = new Entities.LineEntity();
            le.sku = lines.get(i).sku();
            le.quantity = lines.get(i).quantity();
            r.lines.add(le);
        }
        return new Object[] {reservations.save(r), true};
    }

    @Transactional
    public Entities.ReservationEntity get(long id) {
        expire();
        return reservations.findById(id).orElseThrow(() -> new ApiError(404, "NOT_FOUND", "no reservation"));
    }

    @Transactional
    public Entities.ReservationEntity change(long id, String target) {
        expire();
        Entities.ReservationEntity r = reservations.findById(id).orElseThrow(() -> new ApiError(404, "NOT_FOUND", "no reservation"));
        if (r.state.equals(target)) {
            return r;
        }
        if (!r.state.equals("ACTIVE")) {
            throw new ApiError(409, "INVALID_STATE", "bad state");
        }
        for (Entities.LineEntity l : r.lines) {
            Entities.ItemEntity item = items.findById(l.sku).get();
            item.held -= l.quantity;
            if (target.equals("CONFIRMED")) {
                item.sold += l.quantity;
            } else {
                item.available += l.quantity;
            }
        }
        r.state = target;
        return r;
    }

    @Transactional
    public List<Entities.ReservationEntity> all() {
        expire();
        List<Entities.ReservationEntity> list = new ArrayList<>(reservations.findAll());
        list.sort(Comparator.comparing(r -> r.id));
        return list;
    }

    @Transactional
    public long itemCount() {
        return items.count();
    }
}
