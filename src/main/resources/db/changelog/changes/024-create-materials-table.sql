--liquibase formatted sql

--changeset morning-harvest:024-create-materials-table
--comment: 建立原物料主檔表

CREATE TABLE materials (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '原物料ID',
    code VARCHAR(20) COMMENT '原物料編號 (應用層驗證必填且唯一)',
    name VARCHAR(100) COMMENT '原物料名稱 (應用層驗證必填)',
    unit VARCHAR(20) COMMENT '單位: PIECE, PACK, KILOGRAM, LITER, DOZEN, BOX, STRIP, SLICE',
    category VARCHAR(50) COMMENT '分類: BREAD, EGG, MEAT, BEVERAGE, SEASONING, DAIRY, VEGETABLE, FRUIT, OTHER',
    specification VARCHAR(200) COMMENT '規格說明',
    safe_stock_quantity DECIMAL(10, 2) COMMENT '安全庫存量 (應用層預設 0)',
    current_stock_quantity DECIMAL(10, 2) COMMENT '目前庫存量 (應用層預設 0)',
    cost_price DECIMAL(10, 2) COMMENT '成本單價 (應用層預設 0)',
    is_active BOOLEAN COMMENT '是否啟用 (應用層預設 TRUE)',
    note VARCHAR(500) COMMENT '備註',
    created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    INDEX idx_code (code),
    INDEX idx_name (name),
    INDEX idx_category (category),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='原物料主檔表';
