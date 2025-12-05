--liquibase formatted sql

--changeset morning-harvest:029-create-inventory-check-items-table
--comment: 建立庫存盤點明細表

CREATE TABLE inventory_check_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明細ID',
    inventory_check_id BIGINT COMMENT '盤點單ID (應用層關聯驗證)',
    material_id BIGINT COMMENT '原物料ID (應用層關聯驗證)',
    material_code VARCHAR(20) COMMENT '原物料編號 (冗餘儲存)',
    material_name VARCHAR(100) COMMENT '原物料名稱 (冗餘儲存)',
    material_unit VARCHAR(20) COMMENT '原物料單位 (冗餘儲存)',
    system_quantity DECIMAL(10, 2) COMMENT '系統數量 (建立盤點計畫時的庫存數量)',
    actual_quantity DECIMAL(10, 2) COMMENT '實際盤點數量 (盤點時輸入)',
    difference_quantity DECIMAL(10, 2) COMMENT '盤差數量 (應用層計算: actual - system)',
    unit_cost DECIMAL(10, 2) COMMENT '單位成本 (盤點時的原物料成本單價)',
    difference_amount DECIMAL(12, 2) COMMENT '盤差金額 (應用層計算: difference_quantity * unit_cost)',
    is_checked BOOLEAN COMMENT '是否已盤點 (應用層預設 FALSE)',
    note VARCHAR(200) COMMENT '備註',
    created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    INDEX idx_inventory_check_id (inventory_check_id),
    INDEX idx_material_id (material_id),
    INDEX idx_is_checked (is_checked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='庫存盤點明細表';
