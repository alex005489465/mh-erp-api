package com.morningharvest.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Morning Harvest ERP System - 主應用程式
 *
 * 早餐店 ERP 系統 API
 */
@SpringBootApplication
@EnableCaching        // 啟用 Redis 快取
@EnableAsync          // 啟用非同步事件處理
public class ErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
    }

}
