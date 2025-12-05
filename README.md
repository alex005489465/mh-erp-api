# Morning Harvest ERP

早餐店 ERP 系統 API，採用 Spring Boot 3.5 + Java 21 開發。

## 功能

- 商品管理 - 商品、分類、選項、配方
- 原物料管理 - 原物料庫存追蹤
- 供應商管理 - 供應商資料維護
- 進貨管理 - 進貨單與庫存同步
- 庫存盤點 - 盤點作業管理
- 訂單管理 - 訂單處理
- 套餐管理 - 組合商品設定
- POS 系統 - 菜單、桌台管理
- 支付管理 - 收款處理
- 發票管理 - 統編與發票

## 快速開始

### 環境需求

- Java 21
- Maven 3.9+
- MySQL 8.4
- Redis 8.2

### 啟動應用

```bash
# 設定環境變數
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/erp_db?useSSL=false&serverTimezone=Asia/Taipei&allowPublicKeyRetrieval=true"
export SPRING_DATASOURCE_USERNAME="morning_harvest_user"
export SPRING_DATASOURCE_PASSWORD="morning_harvest_dev_password_2024"

# 啟動
mvn spring-boot:run
```

### 存取

- API 文檔: http://localhost:8080/swagger-ui.html
- 健康檢查: http://localhost:8080/actuator/health
