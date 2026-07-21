ALTER TABLE products
ADD CONSTRAINT chk_products_stock_non_negative
CHECK ( stock >= 0 )