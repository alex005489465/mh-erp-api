package com.morningharvest.erp.health;

import com.morningharvest.erp.common.constant.ResponseCode;
import com.morningharvest.erp.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康檢查控制器
 * 提供系統健康狀態、資料庫和 Redis 連線測試
 */
@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Tag(name = "健康檢查", description = "系統健康狀態檢查 API")
public class HealthController {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 基本健康檢查
     */
    @GetMapping
    @Operation(summary = "基本健康檢查", description = "返回 API 運行狀態")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("application", "Morning Harvest ERP API");
        health.put("version", "1.0.0");

        log.info("健康檢查通過");
        return ApiResponse.success("系統運行正常", health);
    }

    /**
     * 資料庫連線測試
     */
    @GetMapping("/database")
    @Operation(summary = "資料庫連線測試", description = "測試 MySQL 連線狀態")
    public ApiResponse<Map<String, Object>> checkDatabase() {
        Map<String, Object> dbHealth = new HashMap<>();

        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(2);

            dbHealth.put("status", isValid ? "UP" : "DOWN");
            dbHealth.put("database", connection.getMetaData().getDatabaseProductName());
            dbHealth.put("version", connection.getMetaData().getDatabaseProductVersion());
            dbHealth.put("url", connection.getMetaData().getURL());
            dbHealth.put("timestamp", LocalDateTime.now());

            if (isValid) {
                log.info("資料庫連線正常: {}", connection.getMetaData().getURL());
                return ApiResponse.success("資料庫連線正常", dbHealth);
            } else {
                log.warn("資料庫連線失敗");
                return ApiResponse.error(ResponseCode.SYSTEM_ERROR, "資料庫連線失敗", dbHealth);
            }

        } catch (Exception e) {
            log.error("資料庫連線測試失敗", e);
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
            return ApiResponse.error(ResponseCode.SYSTEM_ERROR, "資料庫連線失敗: " + e.getMessage(), dbHealth);
        }
    }

    /**
     * Redis 連線測試
     */
    @GetMapping("/redis")
    @Operation(summary = "Redis 連線測試", description = "測試 Redis 連線狀態")
    public ApiResponse<Map<String, Object>> checkRedis() {
        Map<String, Object> redisHealth = new HashMap<>();

        try {
            // 測試 Redis 連線
            String testKey = "health:check:" + System.currentTimeMillis();
            String testValue = "OK";

            redisTemplate.opsForValue().set(testKey, testValue);
            Object result = redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);

            boolean isConnected = testValue.equals(result);

            redisHealth.put("status", isConnected ? "UP" : "DOWN");
            redisHealth.put("timestamp", LocalDateTime.now());

            if (isConnected) {
                log.info("Redis 連線正常");
                return ApiResponse.success("Redis 連線正常", redisHealth);
            } else {
                log.warn("Redis 連線失敗");
                return ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Redis 連線失敗", redisHealth);
            }

        } catch (Exception e) {
            log.error("Redis 連線測試失敗", e);
            redisHealth.put("status", "DOWN");
            redisHealth.put("error", e.getMessage());
            return ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Redis 連線失敗: " + e.getMessage(), redisHealth);
        }
    }

    /**
     * 完整系統檢查
     */
    @GetMapping("/full")
    @Operation(summary = "完整系統檢查", description = "檢查 API、資料庫和 Redis 的完整狀態")
    public ApiResponse<Map<String, Object>> fullCheck() {
        Map<String, Object> fullHealth = new HashMap<>();

        // 檢查 API
        fullHealth.put("api", "UP");
        fullHealth.put("timestamp", LocalDateTime.now());

        // 檢查資料庫
        try (Connection connection = dataSource.getConnection()) {
            fullHealth.put("database", connection.isValid(2) ? "UP" : "DOWN");
        } catch (Exception e) {
            fullHealth.put("database", "DOWN");
            log.error("資料庫檢查失敗", e);
        }

        // 檢查 Redis
        try {
            String testKey = "health:full:check";
            redisTemplate.opsForValue().set(testKey, "OK");
            redisTemplate.delete(testKey);
            fullHealth.put("redis", "UP");
        } catch (Exception e) {
            fullHealth.put("redis", "DOWN");
            log.error("Redis 檢查失敗", e);
        }

        // 判斷整體狀態
        boolean allUp = "UP".equals(fullHealth.get("database")) &&
                        "UP".equals(fullHealth.get("redis"));

        if (allUp) {
            return ApiResponse.success("所有服務運行正常", fullHealth);
        } else {
            return ApiResponse.error(ResponseCode.SYSTEM_ERROR, "部分服務異常", fullHealth);
        }
    }

}
