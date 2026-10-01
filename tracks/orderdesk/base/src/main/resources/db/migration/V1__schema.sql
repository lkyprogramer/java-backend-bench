CREATE TABLE product (
    sku    VARCHAR(32) PRIMARY KEY,
    name   VARCHAR(120) NOT NULL,
    price  DECIMAL(12, 2) NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE stock_level (
    sku      VARCHAR(32) PRIMARY KEY REFERENCES product (sku),
    on_hand  INT NOT NULL,
    reserved INT NOT NULL
);

CREATE TABLE orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     VARCHAR(64) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    total           DECIMAL(12, 2) NOT NULL,
    paid_amount     DECIMAL(12, 2) NOT NULL,
    refunded_amount DECIMAL(12, 2) NOT NULL,
    shipped_at      TIMESTAMP WITH TIME ZONE
);
CREATE INDEX orders_customer ON orders (customer_id);
CREATE INDEX orders_created ON orders (created_at);

CREATE TABLE order_line (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT NOT NULL REFERENCES orders (id),
    sku        VARCHAR(32) NOT NULL REFERENCES product (sku),
    quantity   INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL
);
CREATE INDEX order_line_order ON order_line (order_id);
