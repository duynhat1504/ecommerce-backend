CREATE TABLE inventory_transactions
(
    id              UUID PRIMARY KEY,
    product_id      UUID         NOT NULL,
    type            VARCHAR(50)  NOT NULL,
    quantity_change INTEGER      NOT NULL,
    stock_before    INTEGER      NOT NULL,
    stock_after     INTEGER      NOT NULL,
    reason          VARCHAR(255),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventory_transactions_product
        FOREIGN KEY (product_id)
            REFERENCES products (id),

    CONSTRAINT chk_inventory_transactions_quantity_change
        CHECK (quantity_change <> 0),

    CONSTRAINT chk_inventory_transactions_stock_before
        CHECK (stock_before >= 0),

    CONSTRAINT chk_inventory_transactions_stock_after
        CHECK (stock_after >= 0)
);

CREATE INDEX idx_inventory_transactions_product_id
    ON inventory_transactions (product_id);

CREATE INDEX idx_inventory_transactions_created_at
    ON inventory_transactions (created_at);