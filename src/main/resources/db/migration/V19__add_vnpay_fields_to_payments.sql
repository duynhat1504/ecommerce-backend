ALTER TABLE payments
    ADD COLUMN merchant_txn_ref VARCHAR(100),
    ADD COLUMN gateway_response_code VARCHAR(30),
    ADD COLUMN gateway_transaction_no VARCHAR(100);

ALTER TABLE payments
    ADD CONSTRAINT uk_payments_merchant_txn_ref
        UNIQUE (merchant_txn_ref);