package com.morningharvest.erp.payment.event;

import com.morningharvest.erp.common.event.BaseEvent;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 付款完成事件
 *
 * 當付款交易完成時發布
 */
@Getter
public class PaymentCompletedEvent extends BaseEvent {

    private final Long orderId;
    private final Long transactionId;
    private final BigDecimal amount;

    public PaymentCompletedEvent(Long orderId, Long transactionId, BigDecimal amount) {
        super("PAYMENT");
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.amount = amount;
    }
}
