package com.morningharvest.erp.payment.listener;

import com.morningharvest.erp.order.event.OrderSubmittedEvent;
import com.morningharvest.erp.payment.entity.PaymentTransaction;
import com.morningharvest.erp.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 付款模組事件監聽器
 *
 * 監聽訂單相關事件，處理付款條目的建立
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentTransactionRepository paymentTransactionRepository;

    /**
     * 監聽訂單送出事件
     *
     * 當訂單送出（DRAFT → PENDING_PAYMENT）時，自動建立 PENDING 狀態的付款條目
     */
    @Async
    @EventListener
    @Transactional
    public void onOrderSubmitted(OrderSubmittedEvent event) {
        log.info("收到訂單送出事件, orderId: {}, totalAmount: {}", event.getOrderId(), event.getTotalAmount());

        // 檢查是否已存在付款條目
        if (paymentTransactionRepository.existsByOrderIdAndStatus(event.getOrderId(), "PENDING")) {
            log.warn("付款條目已存在, orderId: {}", event.getOrderId());
            return;
        }

        // 建立 PENDING 狀態的付款條目
        PaymentTransaction transaction = PaymentTransaction.builder()
                .orderId(event.getOrderId())
                .amount(event.getTotalAmount())
                .status("PENDING")
                .paymentMethod("CASH")
                .build();

        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        log.info("付款條目已建立, transactionId: {}, orderId: {}", saved.getId(), event.getOrderId());
    }
}
