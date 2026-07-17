CREATE TABLE order_items
(
    id              UUID PRIMARY KEY,
    order_id        UUID                NOT NULL,
    product_id      UUID,
    product_name    VARCHAR(255)        NOT NULL,
    unit_price      NUMERIC(15, 2)      NOT NULL,
    quantity        INTEGER             NOT NULL,
    subtotal        NUMERIC(15, 2)      NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_order_items_quantity
        CHECK ( quantity > 0 ),

    CONSTRAINT chk_order_items_unit_price
        CHECK ( unit_price >= 0 ),

    CONSTRAINT chk_order_items_subtotal
        check ( subtotal >= 0 ),

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id)
        REFERENCES products (id)
        ON DELETE SET NULL
);

CREATE INDEX idx_order_items_order_id
    ON order_items (order_id);

CREATE INDEX idx_order_items_product_id
    ON order_items (product_id);