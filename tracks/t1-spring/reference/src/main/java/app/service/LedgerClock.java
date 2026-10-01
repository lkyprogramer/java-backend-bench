package app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Logical clock. Manual mode starts from max(persisted manual time, bench.now); the persisted value
 * is loaded by {@link Ledger} after migration. System mode never moves backwards within a process.
 * Mutations happen only under the ledger's write lock.
 */
@Component
public class LedgerClock {

    public static final long MAX_NOW = 8_000_000_000_000_000L;

    private final boolean manual;
    private final long initial;
    private volatile long current;

    public LedgerClock(@Value("${bench.clock:system}") String mode, @Value("${bench.now:1700000000000}") long initial) {
        if (!mode.equals("manual") && !mode.equals("system")) {
            throw new IllegalArgumentException("bench.clock must be manual or system");
        }
        if (initial < 0 || initial > MAX_NOW) {
            throw new IllegalArgumentException("bench.now out of range");
        }
        this.manual = mode.equals("manual");
        this.initial = initial;
        this.current = manual ? initial : System.currentTimeMillis();
    }

    public boolean manual() {
        return manual;
    }

    void recover(long persisted) {
        if (manual) {
            current = Math.max(persisted, initial);
        }
    }

    long now() {
        if (!manual) {
            current = Math.max(current, System.currentTimeMillis());
        }
        return current;
    }

    void set(long next) {
        current = next;
    }
}
