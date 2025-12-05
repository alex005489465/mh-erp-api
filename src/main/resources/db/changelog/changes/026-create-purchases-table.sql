--liquibase formatted sql

--changeset morning-harvest:026-create-purchases-table
--comment: 建立進貨單主表

CREATE TABLE purchases (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '進貨單ID',
    purchase_number VARCHAR(30) COMMENT '進貨單號 (應用層驗證必填且唯一，格式: PO-YYYYMMDD-XXXX)',
    supplier_id BIGINT COMMENT '供應商ID (應用層關聯驗證)',
    supplier_name VARCHAR(100) COMMENT '供應商名稱 (冗餘儲存，查詢用)',
    status VARCHAR(20) COMMENT '狀態: DRAFT, CONFIRMED (應用層預設 DRAFT)',
    total_amount DECIMAL(12, 2) COMMENT '總金額 (應用層計算)',
    purchase_date DATE COMMENT '進貨日期',
    note VARCHAR(500) COMMENT '備註',
    confirmed_at DATETIME(6) COMMENT '確認時間',
    confirmed_by VARCHAR(50) COMMENT '確認人員',
    created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    INDEX idx_purchase_number (purchase_number),
    INDEX idx_supplier_id (supplier_id),
    INDEX idx_status (status),
    INDEX idx_purchase_date (purchase_date),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='進貨單主表';
