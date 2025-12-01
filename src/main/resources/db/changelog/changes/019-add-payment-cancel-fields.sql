--liquibase formatted sql

--changeset morning-harvest:019-add-payment-cancel-fields
--comment: 新增付款交易類型和取消相關欄位

ALTER TABLE payment_transactions
    ADD COLUMN transaction_type VARCHAR(20) COMMENT '交易類型 (PAYMENT/REFUND，應用層預設 PAYMENT)',
    ADD COLUMN is_cancelled BOOLEAN COMMENT '是否已取消 (應用層預設 FALSE)';

CREATE INDEX idx_payment_transactions_type ON payment_transactions (transaction_type);
