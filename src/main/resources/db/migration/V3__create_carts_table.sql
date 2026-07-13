CREATE TABLE carts
(
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_carts_user UNIQUE (user_id),

    CONSTRAINT fk_carts_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
)