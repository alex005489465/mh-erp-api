--liquibase formatted sql

--changeset morning-harvest:014-refactor-order-item-combo
--comment: 重構訂單項目套餐結構 - 新增 COMBO_ITEM 類型，允許 product_id 和 product_name 為 NULL

ALTER TABLE order_items
MODIFY COLUMN product_id BIGINT NULL,
MODIFY COLUMN product_name VARCHAR(100) NULL,
MODIFY COLUMN item_type VARCHAR(15) NOT NULL DEFAULT 'SINGLE';
