CREATE TABLE cart_items
(
    id         UUID PRIMARY KEY,
    cart_id    UUID      NOT NULL,
    product_id UUID      NOT NULL,
    quantity   INTEGER   NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_cart_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT uk_cart_items_cart_product
        UNIQUE (cart_id, product_id),

    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id)
            REFERENCES carts (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_cart_items_product
        FOREIGN KEY (product_id)
            REFERENCES products (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_cart_items_cart_id
    ON cart_items (cart_id);

CREATE INDEX idx_cart_items_product_id
    ON cart_items (product_id);