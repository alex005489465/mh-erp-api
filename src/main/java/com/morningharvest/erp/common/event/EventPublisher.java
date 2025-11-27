package com.morningharvest.erp.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 統一事件發布器
 *
 * 封裝 Spring ApplicationEventPublisher，提供統一的事件發布接口和日誌記錄
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 發布事件
     *
     * @param event 要發布的事件
     * @param <T>   事件類型
     */
    public <T extends BaseEvent> void publish(T event) {
        log.info("發布事件: {}", event);
        try {
            applicationEventPublisher.publishEvent(event);
            log.debug("事件發布成功: eventId={}, eventType={}",
                    event.getEventId(), event.getEventType());
        } catch (Exception e) {
            log.error("事件發布失敗: eventId={}, eventType={}, error={}",
                    event.getEventId(), event.getEventType(), e.getMessage(), e);
            // 輕量級模式：發布失敗記錄日誌但不中斷業務流程
        }
    }

    /**
     * 發布事件（帶自訂描述）
     *
     * @param event       要發布的事件
     * @param description 事件描述
     * @param <T>         事件類型
     */
    public <T extends BaseEvent> void publish(T event, String description) {
        log.info("發布事件 [{}]: {}", description, event);
        publish(event);
    }
}
