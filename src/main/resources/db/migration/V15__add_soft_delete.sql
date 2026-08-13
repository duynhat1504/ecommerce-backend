ALTER TABLE products
    ADD COLUMN deleted_at TIMESTAMP NULL;

ALTER TABLE categories
    ADD COLUMN deleted_at TIMESTAMP NULL;

CREATE INDEX idx_products_deleted_at
    ON products(deleted_at);

CREATE INDEX idx_categories_deleted_at
    ON categories(deleted_at);