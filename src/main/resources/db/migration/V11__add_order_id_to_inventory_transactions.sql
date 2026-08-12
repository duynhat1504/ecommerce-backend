ALTER TABLE inventory_transactions
    ADD COLUMN order_id UUID;

ALTER TABLE inventory_transactions
    ADD CONSTRAINT fk_inventory_transactions_order
        FOREIGN KEY (order_id)
            REFERENCES orders (id)
            ON DELETE SET NULL;

CREATE INDEX idx_inventory_transactions_order_id
    ON inventory_transactions (order_id);