CREATE TABLE item (
    sku       VARCHAR(32) PRIMARY KEY,
    total     BIGINT NOT NULL,
    available BIGINT NOT NULL,
    held      BIGINT NOT NULL,
    sold      BIGINT NOT NULL
);

CREATE SEQUENCE reservation_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE reservation (
    id          BIGINT PRIMARY KEY,
    request_id  VARCHAR(64) NOT NULL,
    state       VARCHAR(16) NOT NULL,
    created_at  BIGINT NOT NULL,
    expires_at  BIGINT NOT NULL,
    ttl_millis  BIGINT NOT NULL
);
CREATE INDEX reservation_due ON reservation (state, expires_at);

CREATE TABLE reservation_line (
    reservation_id BIGINT NOT NULL REFERENCES reservation (id),
    sku            VARCHAR(32) NOT NULL REFERENCES item (sku),
    quantity       BIGINT NOT NULL,
    PRIMARY KEY (reservation_id, sku)
);

CREATE TABLE clock_state (
    id     INT PRIMARY KEY,
    now_ms BIGINT NOT NULL
);
