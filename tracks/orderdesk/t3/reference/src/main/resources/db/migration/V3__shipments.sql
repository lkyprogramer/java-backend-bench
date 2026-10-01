-- Partial shipments. Every order shipped through the v1 API (shipped_at set) becomes exactly one
-- shipment of all its lines. shipped_at is the source of truth, not status: shipped orders may since
-- have been (partially) refunded.
CREATE TABLE shipment (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT NOT NULL REFERENCES orders (id),
    shipped_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX shipment_order ON shipment (order_id);

CREATE TABLE shipment_line (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT NOT NULL REFERENCES shipment (id),
    sku         VARCHAR(32) NOT NULL,
    quantity    INT NOT NULL
);
CREATE INDEX shipment_line_shipment ON shipment_line (shipment_id);

INSERT INTO shipment (order_id, shipped_at)
SELECT id, shipped_at FROM orders WHERE shipped_at IS NOT NULL ORDER BY id;

INSERT INTO shipment_line (shipment_id, sku, quantity)
SELECT s.id, l.sku, l.quantity FROM shipment s JOIN order_line l ON l.order_id = s.order_id ORDER BY s.id, l.sku;
