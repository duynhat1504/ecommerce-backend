ALTER TABLE products
ADD COLUMN search_vector tsvector
GENERATED ALWAYS AS (
    setweight(
        to_tsvector('simple', coalesce(name, '')),
        'A'
    )
    ||
    setweight(
        to_tsvector('simple', coalesce(description, '')),
        'B'
    )
) STORED;

CREATE INDEX idx_products_search_vector
ON products
USING GIN (search_vector);