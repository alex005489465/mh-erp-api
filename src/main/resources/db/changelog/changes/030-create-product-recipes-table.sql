--liquibase formatted sql

--changeset morning-harvest:030-create-product-recipes-table
--comment: 建立商品配方表 (商品與原物料多對多關聯)

CREATE TABLE product_recipes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配方項目ID',
    product_id BIGINT COMMENT '商品ID (應用層驗證必填)',
    product_name VARCHAR(100) COMMENT '商品名稱 (冗餘欄位，便於查詢)',
    material_id BIGINT COMMENT '原物料ID (應用層驗證必填)',
    material_code VARCHAR(20) COMMENT '原物料編號 (冗餘欄位)',
    material_name VARCHAR(100) COMMENT '原物料名稱 (冗餘欄位)',
    quantity DECIMAL(10, 4) COMMENT '用量 (應用層驗證必填且大於0)',
    unit VARCHAR(20) COMMENT '單位 (從原物料帶入)',
    note VARCHAR(200) COMMENT '備註',
    created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    INDEX idx_product_id (product_id),
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品配方表';
