--liquibase formatted sql

--changeset morning-harvest:027-create-purchase-items-table
--comment: 建立進貨明細表

CREATE TABLE purchase_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明細ID',
    purchase_id BIGINT COMMENT '進貨單ID (應用層關聯驗證)',
    material_id BIGINT COMMENT '原物料ID (應用層關聯驗證)',
    material_code VARCHAR(20) COMMENT '原物料編號 (冗餘儲存)',
    material_name VARCHAR(100) COMMENT '原物料名稱 (冗餘儲存)',
    material_unit VARCHAR(20) COMMENT '原物料單位 (冗餘儲存)',
    quantity DECIMAL(10, 2) COMMENT '進貨數量 (應用層驗證必填且大於 0)',
    unit_price DECIMAL(10, 2) COMMENT '單價 (應用層驗證必填且大於等於 0)',
    subtotal DECIMAL(12, 2) COMMENT '小計 (應用層計算: quantity * unit_price)',
    note VARCHAR(200) COMMENT '備註',
    created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    INDEX idx_purchase_id (purchase_id),
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='進貨明細表';
