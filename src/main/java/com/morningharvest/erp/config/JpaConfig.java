package com.morningharvest.erp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 配置
 *
 * 啟用 JPA 審計功能 (自動記錄創建/更新時間)
 * 獨立配置類方便測試時排除
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
