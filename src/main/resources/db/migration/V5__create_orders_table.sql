CREATE TABLE orders
(
    id                  UUID PRIMARY KEY,
    order_code          VARCHAR(40)     NOT NULL,
    user_id             UUID            NOT NULL,
    status              VARCHAR(30)     NOT NULL,
    total_amount        NUMERIC(15, 2)  NOT NULL,

    recipient_name      VARCHAR(150)    NOT NULL,
    phone_number        VARCHAR(30)     NOT NULL,
    shipping_address    VARCHAR(500)    NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_orders_order_code
        UNIQUE (order_code),

    CONSTRAINT chk_orders_total_amount
        CHECK ( total_amount >= 0 ),

    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
);

CREATE INDEX idx_orders_user_id
    ON orders (user_id);

CREATE INDEX idx_orders_status
    ON orders (status);

CREATE INDEX idx_orders_created_at
    ON orders (created_at DESC );