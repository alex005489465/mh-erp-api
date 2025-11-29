--liquibase formatted sql

--changeset morning-harvest:011-create-orders-table
--comment: 建立訂單表

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    order_type VARCHAR(20) NOT NULL DEFAULT 'DINE_IN',
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    note VARCHAR(500),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_status (status),
    INDEX idx_order_type (order_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
