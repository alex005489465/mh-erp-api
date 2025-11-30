--liquibase formatted sql

--changeset morning-harvest:013-add-combo-fields-to-order-items
--comment: 新增套餐相關欄位到訂單項目表

ALTER TABLE order_items
ADD COLUMN item_type VARCHAR(10) NOT NULL DEFAULT 'SINGLE' AFTER note,
ADD COLUMN combo_id BIGINT NULL AFTER item_type,
ADD COLUMN combo_name VARCHAR(100) NULL AFTER combo_id,
ADD COLUMN group_sequence INT NULL AFTER combo_name,
ADD COLUMN combo_price DECIMAL(10, 2) NULL AFTER group_sequence,
ADD INDEX idx_item_type (item_type),
ADD INDEX idx_combo_id (combo_id),
ADD INDEX idx_order_group (order_id, group_sequence);
