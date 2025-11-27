package com.morningharvest.erp.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 非同步任務配置
 *
 * 配置事件監聽器的非同步處理執行緒池
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 配置非同步任務執行器
     *
     * @return 執行緒池執行器
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心執行緒數
        executor.setCorePoolSize(5);

        // 最大執行緒數
        executor.setMaxPoolSize(10);

        // 佇列容量
        executor.setQueueCapacity(100);

        // 執行緒名稱前綴
        executor.setThreadNamePrefix("event-async-");

        // 當執行緒池關閉時，等待所有任務完成
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待時間（秒）
        executor.setAwaitTerminationSeconds(60);

        // 初始化
        executor.initialize();

        log.info("非同步事件執行緒池已初始化: corePoolSize=5, maxPoolSize=10, queueCapacity=100");

        return executor;
    }

    /**
     * 配置非同步異常處理器
     *
     * @return 異常處理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("非同步事件處理發生未捕獲的異常: method={}, params={}, error={}",
                    method.getName(), params, throwable.getMessage(), throwable);
        };
    }
}
