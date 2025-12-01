package com.morningharvest.erp.order.event;

import com.morningharvest.erp.common.event.BaseEvent;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 訂單取消事件
 *
 * 當訂單被取消時發布
 */
@Getter
public class OrderCancelledEvent extends BaseEvent {

    private final Long orderId;
    private final BigDecimal refundAmount;

    public OrderCancelledEvent(Long orderId, BigDecimal refundAmount) {
        super("ORDER");
        this.orderId = orderId;
        this.refundAmount = refundAmount;
    }
}
