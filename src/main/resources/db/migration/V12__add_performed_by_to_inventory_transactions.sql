ALTER TABLE inventory_transactions
    ADD COLUMN performed_by UUID;

ALTER TABLE inventory_transactions
    ADD CONSTRAINT fk_inventory_transactions_performed_by
        FOREIGN KEY (performed_by)
            REFERENCES users (id)
            ON DELETE SET NULL;

CREATE INDEX idx_inventory_transactions_performed_by
    ON inventory_transactions (performed_by);