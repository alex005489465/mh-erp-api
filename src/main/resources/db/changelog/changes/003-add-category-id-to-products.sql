--liquibase formatted sql

--changeset morning-harvest:003-add-category-id-to-products
--comment: 在商品表新增分類 ID 欄位

ALTER TABLE products
ADD COLUMN category_id BIGINT NULL AFTER image_url;

CREATE INDEX idx_category_id ON products (category_id);
