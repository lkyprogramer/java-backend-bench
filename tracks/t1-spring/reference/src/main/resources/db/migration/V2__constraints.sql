-- Defense in depth: the database itself rejects a broken inventory invariant or a duplicate request key.
ALTER TABLE item ADD CONSTRAINT item_conservation
    CHECK (total = available + held + sold AND available >= 0 AND held >= 0 AND sold >= 0);
ALTER TABLE reservation ADD CONSTRAINT reservation_request_id UNIQUE (request_id);
