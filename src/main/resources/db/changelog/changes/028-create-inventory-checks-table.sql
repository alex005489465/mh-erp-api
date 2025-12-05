--liquibase formatted sql

--changeset morning-harvest:028-create-inventory-checks-table
--comment: 建立庫存盤點主表

CREATE TABLE inventory_checks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '盤點單ID',
    check_number VARCHAR(30) COMMENT '盤點單號 (應用層驗證必填且唯一，格式: IC-YYYYMMDD-XXXX)',
    status VARCHAR(20) COMMENT '狀態: PLANNED, IN_PROGRESS, CONFIRMED (應用層預設 PLANNED)',
    check_date DATE COMMENT '盤點日期',
    total_items INT COMMENT '盤點品項數 (應用層計算)',
    total_difference_amount DECIMAL(12, 2) COMMENT '盤差總金額 (應用層計算: 所有明細盤差金額加總)',
    note VARCHAR(500) COMMENT '備註',
    started_at DATETIME(6) COMMENT '開始盤點時間 (PLANNED -> IN_PROGRESS)',
    started_by VARCHAR(50) COMMENT '開始盤點人員',
    confirmed_at DATETIME(6) COMMENT '確認時間 (IN_PROGRESS -> CONFIRMED)',
    confirmed_by VARCHAR(50) COMMENT '確認人員',
    created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    INDEX idx_check_number (check_number),
    INDEX idx_status (status),
    INDEX idx_check_date (check_date),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='庫存盤點主表';
