--liquibase formatted sql

--changeset morning-harvest:016-remove-constraints-add-comments
--comment: 移除所有表的 NOT NULL 和 DEFAULT 約束，新增 COMMENT 註解

-- =====================================================
-- products 表
-- =====================================================
ALTER TABLE products
    MODIFY COLUMN name VARCHAR(100) COMMENT '商品名稱 (應用層驗證必填)',
    MODIFY COLUMN description VARCHAR(500) COMMENT '商品描述',
    MODIFY COLUMN price DECIMAL(10, 2) COMMENT '商品價格 (應用層驗證必填)',
    MODIFY COLUMN image_url VARCHAR(500) COMMENT '商品圖片 URL',
    MODIFY COLUMN category_id BIGINT COMMENT '分類ID (無 FK 約束)',
    MODIFY COLUMN category_name VARCHAR(50) COMMENT '分類名稱 (冗餘儲存)',
    MODIFY COLUMN is_active BOOLEAN COMMENT '是否啟用 (應用層預設 TRUE)',
    MODIFY COLUMN sort_order INT COMMENT '排序順序 (應用層預設 0)',
    MODIFY COLUMN created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    MODIFY COLUMN updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    COMMENT = '商品表';

-- =====================================================
-- product_categories 表
-- =====================================================
ALTER TABLE product_categories
    MODIFY COLUMN name VARCHAR(50) COMMENT '分類名稱 (應用層驗證必填)',
    MODIFY COLUMN description VARCHAR(200) COMMENT '分類描述',
    MODIFY COLUMN sort_order INT COMMENT '排序順序 (應用層預設 0)',
    MODIFY COLUMN is_active BOOLEAN COMMENT '是否啟用 (應用層預設 TRUE)',
    MODIFY COLUMN created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    MODIFY COLUMN updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    COMMENT = '商品分類表';

-- =====================================================
-- option_template_groups 表
-- =====================================================
ALTER TABLE option_template_groups
    MODIFY COLUMN name VARCHAR(50) COMMENT '選項群組名稱 (應用層驗證必填)',
    MODIFY COLUMN min_selections INT COMMENT '最小選擇數 (應用層預設 0)',
    MODIFY COLUMN max_selections INT COMMENT '最大選擇數 (應用層預設 1)',
    MODIFY COLUMN sort_order INT COMMENT '排序順序 (應用層預設 0)',
    MODIFY COLUMN is_active BOOLEAN COMMENT '是否啟用 (應用層預設 TRUE)',
    MODIFY COLUMN created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    MODIFY COLUMN updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    COMMENT = '選項範本群組表';

-- =====================================================
-- option_template_values 表
-- =====================================================
ALTER TABLE option_template_values
    MODIFY COLUMN group_id BIGINT COMMENT '所屬群組ID (應用層驗證必填)',
    MODIFY COLUMN name VARCHAR(50) COMMENT '選項值名稱 (應用層驗證必填)',
    MODIFY COLUMN price_adjustment DECIMAL(10,2) COMMENT '價格調整 (應用層預設 0.00)',
    MODIFY COLUMN sort_order INT COMMENT '排序順序 (應用層預設 0)',
    MODIFY COLUMN is_active BOOLEAN COMMENT '是否啟用 (應用層預設 TRUE)',
    MODIFY COLUMN created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    MODIFY COLUMN updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    COMMENT = '選項範本值表';

-- =====================================================
-- product_option_groups 表
-- =====================================================
ALTER TABLE product_option_groups
    MODIFY COLUMN product_id BIGINT COMMENT '所屬商品ID (應用層驗證必填)',
    MODIFY COLUMN name VARCHAR(50) COMMENT '選項群組名稱 (應用層驗證必填)',
    MODIFY COLUMN min_selections INT COMMENT '最小選擇數 (應用層預設 0)',
    MODIFY COLUMN max_selections INT COMMENT '最大選擇數 (應用層預設 1)',
    MODIFY COLUMN sort_order INT COMMENT '排序順序 (應用層預設 0)',
    MODIFY COLUMN is_active BOOLEAN COMMENT '是否啟用 (應用層預設 TRUE)',
    MODIFY COLUMN created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    MODIFY COLUMN updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    COMMENT = '商品選項群組表';

-- =====================================================
-- product_option_values 表
-- =====================================================
ALTER TABLE product_option_values
    MODIFY COLUMN group_id BIGINT COMMENT '所屬群組ID (應用層驗證必填)',
    MODIFY COLUMN name VARCHAR(50) COMMENT '選項值名稱 (應用層驗證必填)',
    MODIFY COLUMN price_adjustment DECIMAL(10,2) COMMENT '價格調整 (應用層預設 0.00)',
    MODIFY COLUMN sort_order INT COMMENT '排序順序 (應用層預設 0)',
    MODIFY COLUMN is_active BOOLEAN COMMENT '是否啟用 (應用層預設 TRUE)',
    MODIFY COLUMN created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    MODIFY COLUMN updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    COMMENT = '商品選項值表';

-- =====================================================
-- combos 表
-- =====================================================
ALTER TABLE combos
    MODIFY COLUMN name VARCHAR(100) COMMENT '套餐名稱 (應用層驗證必填)',
    MODIFY COLUMN description VARCHAR(500) COMMENT '套餐描述',
    MODIFY COLUMN price DECIMAL(10,2) COMMENT '套餐價格 (應用層驗證必填)',
    MODIFY COLUMN image_url VARCHAR(500) COMMENT '套餐圖片 URL',
    MODIFY COLUMN category_id BIGINT COMMENT '分類ID (無 FK 約束)',
    MODIFY COLUMN category_name VARCHAR(50) COMMENT '分類名稱 (冗餘儲存)',
    MODIFY COLUMN is_active BOOLEAN COMMENT '是否啟用 (應用層預設 TRUE)',
    MODIFY COLUMN sort_order INT COMMENT '排序順序 (應用層預設 0)',
    MODIFY COLUMN created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    MODIFY COLUMN updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    COMMENT = '套餐組合表';

-- =====================================================
-- combo_items 表
-- =====================================================
ALTER TABLE combo_items
    MODIFY COLUMN combo_id BIGINT COMMENT '所屬套餐ID (應用層驗證必填)',
    MODIFY COLUMN product_id BIGINT COMMENT '商品ID (應用層驗證必填)',
    MODIFY COLUMN product_name VARCHAR(100) COMMENT '商品名稱 (冗餘儲存)',
    MODIFY COLUMN quantity INT COMMENT '數量 (應用層預設 1)',
    MODIFY COLUMN sort_order INT COMMENT '排序順序 (應用層預設 0)',
    MODIFY COLUMN created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    MODIFY COLUMN updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    COMMENT = '套餐項目表';

-- =====================================================
-- orders 表
-- =====================================================
ALTER TABLE orders
    MODIFY COLUMN status VARCHAR(20) COMMENT '訂單狀態 (應用層預設 DRAFT)',
    MODIFY COLUMN order_type VARCHAR(20) COMMENT '訂單類型 (應用層預設 DINE_IN)',
    MODIFY COLUMN total_amount DECIMAL(10, 2) COMMENT '總金額 (應用層預設 0.00)',
    MODIFY COLUMN note VARCHAR(500) COMMENT '訂單備註',
    MODIFY COLUMN created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    MODIFY COLUMN updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    COMMENT = '訂單表';

-- =====================================================
-- order_items 表
-- =====================================================
ALTER TABLE order_items
    MODIFY COLUMN order_id BIGINT COMMENT '所屬訂單ID (應用層驗證必填)',
    MODIFY COLUMN product_id BIGINT COMMENT '商品ID',
    MODIFY COLUMN product_name VARCHAR(100) COMMENT '商品名稱 (冗餘儲存)',
    MODIFY COLUMN unit_price DECIMAL(10, 2) COMMENT '單價 (應用層驗證必填)',
    MODIFY COLUMN quantity INT COMMENT '數量 (應用層預設 1)',
    MODIFY COLUMN subtotal DECIMAL(10, 2) COMMENT '小計 (應用層預設 0.00)',
    MODIFY COLUMN options JSON COMMENT '選項 JSON',
    MODIFY COLUMN options_amount DECIMAL(10, 2) COMMENT '選項金額 (應用層預設 0.00)',
    MODIFY COLUMN note VARCHAR(200) COMMENT '項目備註',
    MODIFY COLUMN item_type VARCHAR(15) COMMENT '項目類型 (應用層預設 SINGLE)',
    MODIFY COLUMN combo_id BIGINT COMMENT '套餐ID',
    MODIFY COLUMN combo_name VARCHAR(100) COMMENT '套餐名稱 (冗餘儲存)',
    MODIFY COLUMN group_sequence INT COMMENT '群組序號',
    MODIFY COLUMN combo_price DECIMAL(10, 2) COMMENT '套餐價格',
    MODIFY COLUMN created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    MODIFY COLUMN updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    COMMENT = '訂單項目表';

-- =====================================================
-- payment_transactions 表
-- =====================================================
ALTER TABLE payment_transactions
    MODIFY COLUMN order_id BIGINT COMMENT '所屬訂單ID (應用層驗證必填)',
    MODIFY COLUMN payment_method VARCHAR(20) COMMENT '付款方式 (應用層預設 CASH)',
    MODIFY COLUMN status VARCHAR(20) COMMENT '交易狀態 (應用層預設 COMPLETED)',
    MODIFY COLUMN amount DECIMAL(10, 2) COMMENT '交易金額 (應用層驗證必填)',
    MODIFY COLUMN reference_no VARCHAR(100) COMMENT '參考編號',
    MODIFY COLUMN note VARCHAR(500) COMMENT '交易備註',
    MODIFY COLUMN transaction_time DATETIME(6) COMMENT '交易時間 (應用層驗證必填)',
    MODIFY COLUMN created_at DATETIME(6) COMMENT '建立時間 (應用層設定)',
    MODIFY COLUMN updated_at DATETIME(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新時間',
    COMMENT = '付款交易表';
