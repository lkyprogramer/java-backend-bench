package app.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import app.domain.ClockRepository;
import app.domain.ClockState;
import app.domain.Item;
import app.domain.ItemRepository;
import app.domain.Line;
import app.domain.Reservation;
import app.domain.ReservationRepository;
import app.domain.State;
import jakarta.persistence.PersistenceException;

/**
 * All business operations. One process-wide lock serializes every operation and is held until the
 * database transaction has committed or rolled back, so no request observes another's uncommitted
 * state and no two transactions race on the same rows. Each operation first applies due expiries
 * in the same transaction. A commit failure surfaces as 503 STORAGE_ERROR with nothing committed;
 * the only in-memory state (the clock) changes after a successful commit.
 */
@Service
public class Ledger {

    private final ReentrantLock lock = new ReentrantLock();
    private final TransactionTemplate tx;
    private final ItemRepository items;
    private final ReservationRepository reservations;
    private final ClockRepository clocks;
    private final LedgerClock clock;

    public Ledger(PlatformTransactionManager manager, ItemRepository items, ReservationRepository reservations,
            ClockRepository clocks, LedgerClock clock) {
        this.tx = new TransactionTemplate(manager);
        this.items = items;
        this.reservations = reservations;
        this.clocks = clocks;
        this.clock = clock;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void recoverClock() {
        clock.recover(clocks.findById(ClockState.SINGLETON).map(ClockState::getNowMs).orElse(0L));
    }

    private <T> T run(Supplier<T> work) {
        lock.lock();
        try {
            return tx.execute(status -> {
                expireDue(clock.now());
                return work.get();
            });
        } catch (TransactionException | DataAccessException | PersistenceException e) {
            throw new ApiException(503, "STORAGE_ERROR", "Storage operation failed; nothing was committed");
        } finally {
            lock.unlock();
        }
    }

    private void expireDue(long now) {
        List<Reservation> due = reservations.findByStateAndExpiresAtLessThanEqualOrderById(State.ACTIVE, Mutant.M04.on() ? now - 1 : now);
        for (Reservation r : due) {
            release(r);
            r.setState(State.EXPIRED);
        }
    }

    private void release(Reservation r) {
        Map<String, Item> bySku = itemsFor(r.getLines());
        for (Line line : r.getLines()) {
            bySku.get(line.sku()).release(line.quantity());
        }
    }

    private Map<String, Item> itemsFor(List<Line> lines) {
        Map<String, Item> bySku = new LinkedHashMap<>();
        for (Item item : items.findBySkuIn(lines.stream().map(Line::sku).toList())) {
            bySku.put(item.getSku(), item);
        }
        return bySku;
    }

    public Map<String, Object> createItem(String sku, long quantity) {
        return run(() -> {
            if (items.existsById(sku)) {
                throw ApiException.conflict("ITEM_EXISTS", "Item already exists");
            }
            return Views.item(items.save(new Item(sku, quantity)));
        });
    }

    public Map<String, Object> item(String sku) {
        return run(() -> Views.item(items.findById(sku).orElseThrow(() -> ApiException.notFound("Item not found"))));
    }

    public record Created(boolean created, Map<String, Object> view) {
    }

    public Created reserve(String requestId, List<Line> requested, long ttl) {
        List<Line> lines = new ArrayList<>(requested);
        lines.sort(Comparator.comparing(Line::sku));
        Set<String> seen = new HashSet<>();
        for (Line line : lines) {
            if (!seen.add(line.sku())) {
                throw ApiException.invalid("Duplicate sku in lines");
            }
        }
        Object[] partial = {null};
        Created result = run(() -> {
            var previous = reservations.findFirstByRequestIdOrderById(requestId);
            if (previous.isPresent() && !Mutant.M02.on()) {
                Reservation r = previous.get();
                if (!r.samePayload(lines, ttl) && !Mutant.M03.on()) {
                    throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "Request key has a different payload");
                }
                return new Created(false, Views.reservation(r));
            }
            Map<String, Item> bySku = itemsFor(lines);
            if (bySku.size() != lines.size()) {
                throw ApiException.notFound("Item not found");
            }
            List<Line> held = new ArrayList<>();
            for (Line line : lines) {
                if (bySku.get(line.sku()).getAvailable() < line.quantity() && !Mutant.M01.on()) {
                    if (Mutant.M08.on()) {
                        held.forEach(done -> bySku.get(done.sku()).hold(done.quantity()));
                        partial[0] = ApiException.conflict("INSUFFICIENT_STOCK", "Not enough stock");
                        return null;
                    }
                    throw ApiException.conflict("INSUFFICIENT_STOCK", "Not enough stock");
                }
                held.add(line);
            }
            lines.forEach(line -> bySku.get(line.sku()).hold(line.quantity()));
            return new Created(true, Views.reservation(reservations.save(new Reservation(requestId, lines, ttl, clock.now()))));
        });
        if (partial[0] != null) {
            throw (ApiException) partial[0];
        }
        return result;
    }

    public Map<String, Object> reservation(long id) {
        return run(() -> Views.reservation(find(id)));
    }

    private Reservation find(long id) {
        return reservations.findById(id).orElseThrow(() -> ApiException.notFound("Reservation not found"));
    }

    public Map<String, Object> transition(long id, State target) {
        return run(() -> {
            Reservation r = find(id);
            boolean repeatCancelMutant = Mutant.M05.on() && target == State.CANCELLED && r.getState() == State.CANCELLED;
            if (r.getState() == target && !repeatCancelMutant) {
                return Views.reservation(r);
            }
            boolean cancelledConfirmMutant = Mutant.M07.on() && target == State.CONFIRMED && r.getState() == State.CANCELLED;
            if (r.getState() != State.ACTIVE && !repeatCancelMutant && !cancelledConfirmMutant) {
                throw ApiException.conflict("INVALID_STATE", "Invalid state transition");
            }
            Map<String, Item> bySku = itemsFor(r.getLines());
            for (Line line : r.getLines()) {
                Item item = bySku.get(line.sku());
                if (target == State.CONFIRMED) {
                    item.sell(line.quantity());
                    if (Mutant.M06.on()) {
                        item.corruptReturn(line.quantity());
                    }
                } else {
                    item.release(line.quantity());
                }
            }
            r.setState(target);
            return Views.reservation(r);
        });
    }

    public long advance(long millis) {
        Long[] next = {null};
        run(() -> {
            long now = clock.now();
            if (now > LedgerClock.MAX_NOW - millis) {
                throw ApiException.invalid("Clock would exceed the maximum");
            }
            next[0] = now + millis;
            ClockState state = clocks.findById(ClockState.SINGLETON).orElseGet(() -> new ClockState(0));
            state.setNowMs(next[0]);
            clocks.save(state);
            return null;
        });
        clock.set(next[0]);
        return next[0];
    }

    public Map<String, Object> stats() {
        return run(() -> {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("items", items.count());
            Map<State, Long> counts = new LinkedHashMap<>();
            for (State s : State.values()) {
                counts.put(s, 0L);
            }
            for (Object[] row : reservations.countByState()) {
                counts.put((State) row[0], (Long) row[1]);
            }
            out.put("reservations", counts.values().stream().mapToLong(Long::longValue).sum());
            out.put("active", counts.get(State.ACTIVE));
            out.put("confirmed", counts.get(State.CONFIRMED));
            out.put("cancelled", counts.get(State.CANCELLED));
            out.put("expired", counts.get(State.EXPIRED));
            out.put("now", clock.now());
            return out;
        });
    }

    public Map<String, Object> list(long after, int limit, State state) {
        return run(() -> {
            Limit window = Limit.of(limit + 1);
            List<Reservation> page = state == null || Mutant.M10.on()
                    ? reservations.findByIdGreaterThanOrderById(after, window)
                    : reservations.findByIdGreaterThanAndStateOrderById(after, state, window);
            boolean more = page.size() > limit;
            List<Map<String, Object>> views = page.stream().limit(limit).map(Views::reservation).toList();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("items", views);
            out.put("nextAfter", more ? views.get(views.size() - 1).get("id") : null);
            return out;
        });
    }
}
