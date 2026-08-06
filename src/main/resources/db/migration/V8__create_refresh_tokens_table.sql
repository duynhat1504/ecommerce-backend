CREATE TABLE public.refresh_tokens
(
    id                     UUID         NOT NULL,
    user_id                UUID         NOT NULL,
    token_hash             VARCHAR(64)  NOT NULL,
    expires_at             TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    revoked_at             TIMESTAMP(6) WITHOUT TIME ZONE,
    replaced_by_token_hash VARCHAR(64),
    created_at             TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT refresh_tokens_pkey
        PRIMARY KEY (id),

    CONSTRAINT uk_refresh_tokens_token_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
            REFERENCES public.users (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_refresh_tokens_expiration
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_refresh_tokens_user_id
    ON public.refresh_tokens (user_id);

CREATE INDEX idx_refresh_tokens_expires_at
    ON public.refresh_tokens (expires_at);