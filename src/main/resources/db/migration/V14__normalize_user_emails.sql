DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM users
        GROUP BY LOWER(BTRIM(email))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Cannot normalize users: duplicate emails exist ignoring case and surrounding spaces';
END IF;
END
$$;

UPDATE users
SET email = LOWER(BTRIM(email))
WHERE email <> LOWER(BTRIM(email));

CREATE UNIQUE INDEX ux_users_email_normalized
    ON users (LOWER(BTRIM(email)));