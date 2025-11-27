--liquibase formatted sql

--changeset morning-harvest:004-add-category-name-to-products
--comment: 在商品表新增分類名稱欄位（冗餘儲存）

ALTER TABLE products
ADD COLUMN category_name VARCHAR(50) NULL AFTER category_id;

-- 初始化現有資料的 category_name
UPDATE products p
SET p.category_name = (
    SELECT pc.name FROM product_categories pc WHERE pc.id = p.category_id
)
WHERE p.category_id IS NOT NULL;
