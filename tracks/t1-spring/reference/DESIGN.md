# Reference design

- **Schema**: Flyway `V1__init.sql` (item, reservation, reservation_line, clock_state, `reservation_seq`) and
  `V2__constraints.sql` (inventory conservation CHECK, unique request key). Hibernate runs with `ddl-auto=validate`.
- **Transactions**: every operation runs in one `TransactionTemplate` transaction inside a process-wide
  `ReentrantLock`. The lock is released only after commit/rollback, so no request sees uncommitted state and
  check-then-act on stock or request keys cannot race. A commit failure maps to 503 STORAGE_ERROR; the only
  in-memory state (the manual clock) changes after commit.
- **Expiry**: each operation first expires due ACTIVE reservations (`state, expires_at` index) in the same
  transaction, so every observable read reflects time and replay needs no special path.
- **Idempotency**: request key lookup before any stock check; equality compares sorted lines element-wise
  (Hibernate's `PersistentBag` has identity `equals`).
- **Reads**: list pages fetch `limit+1` rows by id and load lines with batch fetching (one extra query).
- **HTTP**: bodies are read manually in contract order (415 → 413 → UTF-8/JSON → fields) and inspected as a
  Jackson tree so no scalar coercion applies; all errors, including `/error`, use the frozen envelope.
- **Trade-off**: the global lock caps write concurrency; it is the documented, simplest correct baseline.
