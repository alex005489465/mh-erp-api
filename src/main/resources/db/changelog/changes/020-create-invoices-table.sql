--liquibase formatted sql

--changeset morning-harvest:020-create-invoices-table
--comment: 建立發票主表

CREATE TABLE invoices (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '發票記錄 ID',
    order_id BIGINT COMMENT '關聯訂單 ID (應用層驗證)',
    payment_transaction_id BIGINT COMMENT '關聯付款交易 ID (應用層驗證)',

    invoice_number VARCHAR(20) COMMENT '發票號碼 (如 AB-12345678)',
    invoice_date DATE COMMENT '發票日期',
    invoice_period VARCHAR(5) COMMENT '發票期別 (如 11312 = 113年11-12月)',

    invoice_type VARCHAR(20) COMMENT '發票類型: B2C / B2B',
    issue_type VARCHAR(20) COMMENT '開立類型: ELECTRONIC (電子) / PAPER (紙本)',

    buyer_identifier VARCHAR(10) COMMENT '買方統編 (B2B 必填，8 碼)',
    buyer_name VARCHAR(100) COMMENT '買方名稱 (B2B 時填寫)',

    carrier_type VARCHAR(20) COMMENT '載具類型: MOBILE_BARCODE / NATURAL_PERSON / MEMBER / NONE',
    carrier_value VARCHAR(64) COMMENT '載具號碼 (如 /ABC1234)',

    is_donated BOOLEAN COMMENT '是否捐贈 (應用層預設 FALSE)',
    donate_code VARCHAR(10) COMMENT '愛心碼 (捐贈對象代碼)',

    sales_amount DECIMAL(10, 2) COMMENT '銷售額 (未稅)',
    tax_amount DECIMAL(10, 2) COMMENT '稅額',
    total_amount DECIMAL(10, 2) COMMENT '總金額 (含稅)',

    status VARCHAR(20) COMMENT '狀態: ISSUED / VOID / FAILED',
    external_invoice_id VARCHAR(50) COMMENT '發票服務回傳的外部 ID',
    issue_result_code VARCHAR(20) COMMENT '開立結果代碼',
    issue_result_message VARCHAR(500) COMMENT '開立結果訊息',
    issued_at DATETIME(6) COMMENT '開立成功時間',

    is_printed BOOLEAN COMMENT '是否已列印 (應用層預設 FALSE)',
    print_count INT COMMENT '列印次數 (應用層預設 0)',
    last_printed_at DATETIME(6) COMMENT '最後列印時間',

    is_voided BOOLEAN COMMENT '是否已作廢 (應用層預設 FALSE)',
    voided_at DATETIME(6) COMMENT '作廢時間',
    void_reason VARCHAR(200) COMMENT '作廢原因',

    created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',

    INDEX idx_invoices_order_id (order_id),
    INDEX idx_invoices_invoice_number (invoice_number),
    INDEX idx_invoices_invoice_date (invoice_date),
    INDEX idx_invoices_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='發票主表';
