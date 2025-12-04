--liquibase formatted sql

--changeset morning-harvest:021-create-invoice-items-table
--comment: 建立發票明細表

CREATE TABLE invoice_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明細 ID',
    invoice_id BIGINT COMMENT '關聯發票 ID (應用層驗證)',
    sequence INT COMMENT '項次序號 (1, 2, 3...)',
    description VARCHAR(200) COMMENT '品名',
    quantity DECIMAL(10, 2) COMMENT '數量',
    unit_price DECIMAL(10, 2) COMMENT '單價',
    amount DECIMAL(10, 2) COMMENT '金額',
    note VARCHAR(200) COMMENT '備註 (如客製化選項)',
    created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',

    INDEX idx_invoice_items_invoice_id (invoice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='發票明細表';
