DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM categories
        GROUP BY LOWER(BTRIM(name))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Cannot normalize categories: duplicate category names exist ignoring case and surrounding spaces';
END IF;
END
$$;

UPDATE categories
SET name = BTRIM(name)
WHERE name <> BTRIM(name);

CREATE UNIQUE INDEX ux_categories_name_normalized
    ON categories (LOWER(BTRIM(name)));