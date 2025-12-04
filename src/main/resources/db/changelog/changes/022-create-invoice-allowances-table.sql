--liquibase formatted sql

--changeset morning-harvest:022-create-invoice-allowances-table
--comment: 建立發票折讓記錄表

CREATE TABLE invoice_allowances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '折讓記錄 ID',
    invoice_id BIGINT COMMENT '原發票 ID (應用層驗證)',
    allowance_number VARCHAR(20) COMMENT '折讓單號碼',
    allowance_date DATE COMMENT '折讓日期',

    sales_amount DECIMAL(10, 2) COMMENT '折讓銷售額 (未稅)',
    tax_amount DECIMAL(10, 2) COMMENT '折讓稅額',
    total_amount DECIMAL(10, 2) COMMENT '折讓總額 (含稅)',

    status VARCHAR(20) COMMENT '狀態: ISSUED / FAILED',
    reason VARCHAR(200) COMMENT '折讓原因',
    external_allowance_id VARCHAR(50) COMMENT '外部折讓 ID',
    result_code VARCHAR(20) COMMENT '折讓結果代碼',
    result_message VARCHAR(500) COMMENT '折讓結果訊息',

    created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',

    INDEX idx_invoice_allowances_invoice_id (invoice_id),
    INDEX idx_invoice_allowances_allowance_number (allowance_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='發票折讓記錄表';
