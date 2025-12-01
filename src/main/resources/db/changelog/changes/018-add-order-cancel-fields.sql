--liquibase formatted sql

--changeset morning-harvest:018-add-order-cancel-fields
--comment: 新增訂單取消相關欄位

ALTER TABLE orders
    ADD COLUMN is_cancelled BOOLEAN COMMENT '是否已取消 (應用層預設 FALSE)',
    ADD COLUMN cancelled_at DATETIME(6) COMMENT '取消時間',
    ADD COLUMN cancel_reason VARCHAR(500) COMMENT '取消原因';

CREATE INDEX idx_orders_is_cancelled ON orders (is_cancelled);
