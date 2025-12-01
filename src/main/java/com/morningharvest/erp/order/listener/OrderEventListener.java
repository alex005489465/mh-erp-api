package com.morningharvest.erp.order.listener;

import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.repository.OrderRepository;
import com.morningharvest.erp.payment.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 訂單模組事件監聽器
 *
 * 監聽付款相關事件，處理訂單狀態的更新
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderRepository orderRepository;

    /**
     * 監聽付款完成事件
     *
     * 當付款完成時，更新訂單狀態為 PAID
     */
    @Async
    @EventListener
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("收到付款完成事件, orderId: {}, transactionId: {}", event.getOrderId(), event.getTransactionId());

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在: " + event.getOrderId()));

        // 驗證訂單狀態
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            log.warn("訂單狀態不是待付款，跳過更新, orderId: {}, status: {}", event.getOrderId(), order.getStatus());
            return;
        }

        // 更新訂單狀態為 PAID
        order.setStatus("PAID");
        orderRepository.save(order);
        log.info("訂單狀態已更新為 PAID, orderId: {}", event.getOrderId());
    }
}
