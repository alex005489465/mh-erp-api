--liquibase formatted sql

--changeset morning-harvest:023-create-dining-tables-table
--comment: 建立桌位表

CREATE TABLE dining_tables (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '桌位ID',
    table_number VARCHAR(20) COMMENT '桌號 (應用層驗證必填)',
    capacity INT COMMENT '容納人數 (應用層預設 4)',
    status VARCHAR(20) COMMENT '桌位狀態: AVAILABLE, OCCUPIED (應用層預設 AVAILABLE)',
    current_order_id BIGINT COMMENT '當前訂單ID (佔用時)',
    is_active BOOLEAN COMMENT '是否啟用 (應用層預設 TRUE)',
    note VARCHAR(200) COMMENT '備註',
    created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    INDEX idx_table_number (table_number),
    INDEX idx_status (status),
    INDEX idx_current_order_id (current_order_id),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='桌位表';
