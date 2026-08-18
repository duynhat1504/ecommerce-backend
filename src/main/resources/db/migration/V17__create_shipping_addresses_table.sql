CREATE TABLE shipping_addresses (
    id              UUID            PRIMARY KEY,
    user_id         UUID            NOT NULL,
    recipient_name  VARCHAR(150)    NOT NULL,
    phone_number    VARCHAR(30)     NOT NULL,
    province        VARCHAR(100)    NOT NULL,
    district        VARCHAR(100)    NOT NULL,
    ward            VARCHAR(100)    NOT NULL,
    address_line    VARCHAR(255)    NOT NULL,
    is_default      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL,

    CONSTRAINT fk_shipping_addresses_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE
);

CREATE INDEX idx_shipping_addresses_user_id
    ON shipping_addresses(user_id);

CREATE UNIQUE INDEX uk_shipping_addresses_default_per_user
    ON shipping_addresses(user_id)
    WHERE is_default = TRUE;