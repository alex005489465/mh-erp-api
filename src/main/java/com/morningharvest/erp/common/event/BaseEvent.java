package com.morningharvest.erp.common.event;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 事件基礎類別
 *
 * 所有業務事件都應繼承此類別，提供統一的事件元數據
 */
@Getter
public abstract class BaseEvent {

    /**
     * 事件唯一識別碼
     */
    private final String eventId;

    /**
     * 事件發生時間
     */
    private final LocalDateTime occurredAt;

    /**
     * 事件來源（模組名稱）
     */
    private final String source;

    /**
     * 建構函式
     *
     * @param source 事件來源模組
     */
    protected BaseEvent(String source) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.source = source;
    }

    /**
     * 取得事件類型名稱
     *
     * @return 事件類型的簡單類別名稱
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return String.format("%s[eventId=%s, source=%s, occurredAt=%s]",
                getEventType(), eventId, source, occurredAt);
    }
}
