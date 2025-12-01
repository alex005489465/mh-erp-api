--liquibase formatted sql
--changeset morning-harvest:017-add-payment-fields
--comment: 付款交易表新增實收金額和找零金額欄位

ALTER TABLE payment_transactions
    ADD COLUMN amount_received DECIMAL(10, 2) COMMENT '實收金額',
    ADD COLUMN change_amount DECIMAL(10, 2) COMMENT '找零金額';
