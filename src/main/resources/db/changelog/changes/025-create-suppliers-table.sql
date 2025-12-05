--liquibase formatted sql

--changeset morning-harvest:025-create-suppliers-table
--comment: 建立供應商主檔表

CREATE TABLE suppliers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '供應商ID',
    code VARCHAR(20) COMMENT '供應商編號 (應用層驗證必填且唯一)',
    name VARCHAR(100) COMMENT '供應商名稱 (應用層驗證必填)',
    short_name VARCHAR(50) COMMENT '簡稱',
    contact_person VARCHAR(50) COMMENT '聯絡人',
    phone VARCHAR(20) COMMENT '電話',
    mobile VARCHAR(20) COMMENT '手機',
    fax VARCHAR(20) COMMENT '傳真',
    email VARCHAR(100) COMMENT '電子郵件',
    tax_id VARCHAR(20) COMMENT '統一編號',
    address VARCHAR(200) COMMENT '地址',
    payment_terms VARCHAR(50) COMMENT '付款條件: COD, NET30, NET60, NET90',
    bank_name VARCHAR(100) COMMENT '銀行名稱',
    bank_account VARCHAR(50) COMMENT '銀行帳號',
    is_active BOOLEAN COMMENT '是否啟用 (應用層預設 TRUE)',
    note VARCHAR(500) COMMENT '備註',
    created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    INDEX idx_code (code),
    INDEX idx_name (name),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='供應商主檔表';
