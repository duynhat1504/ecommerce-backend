ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN;

UPDATE users
SET email_verified = TRUE;

ALTER TABLE users
    ALTER COLUMN email_verified SET NOT NULL;

ALTER TABLE users
    ALTER COLUMN email_verified SET DEFAULT FALSE;


CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_email_verification_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_tokens_expires_at
    ON email_verification_tokens(expires_at);