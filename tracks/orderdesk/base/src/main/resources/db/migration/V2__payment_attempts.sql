CREATE TABLE payment_attempt (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT NOT NULL REFERENCES orders (id),
    amount     DECIMAL(12, 2) NOT NULL,
    outcome    VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
