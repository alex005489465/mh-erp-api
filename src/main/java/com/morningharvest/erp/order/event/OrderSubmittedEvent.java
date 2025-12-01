package com.morningharvest.erp.order.event;

import com.morningharvest.erp.common.event.BaseEvent;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 訂單送出事件
 *
 * 當訂單從 DRAFT 狀態變更為 PENDING_PAYMENT 時發布
 */
@Getter
public class OrderSubmittedEvent extends BaseEvent {

    private final Long orderId;
    private final BigDecimal totalAmount;

    public OrderSubmittedEvent(Long orderId, BigDecimal totalAmount) {
        super("ORDER");
        this.orderId = orderId;
        this.totalAmount = totalAmount;
    }
}
