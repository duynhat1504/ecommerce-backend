CREATE TABLE payments (
    id                  UUID            PRIMARY KEY,
    order_id            UUID            NOT NULL,
    amount              NUMERIC(15, 2)  NOT NULL,
    method              VARCHAR(30)     NOT NULL,
    status              VARCHAR(30)     NOT NULL,
    idempotency_key     VARCHAR(100)    NOT NULL,
    transaction_code    VARCHAR(100),
    failure_reason      VARCHAR(500),
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL,

    CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id)
            REFERENCES orders(id),

    CONSTRAINT uk_payments_idempotency_key
        UNIQUE (idempotency_key),

    CONSTRAINT ck_payments_amount_positive
        CHECK (amount > 0)
);

CREATE INDEX idx_payments_order_id
    ON payments(order_id);

CREATE INDEX idx_payments_status
    ON payments(status);