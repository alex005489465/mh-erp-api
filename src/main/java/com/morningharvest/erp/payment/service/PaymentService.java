package com.morningharvest.erp.payment.service;

import com.morningharvest.erp.common.event.EventPublisher;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.order.dto.OrderDTO;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.repository.OrderRepository;
import com.morningharvest.erp.payment.dto.CheckoutRequest;
import com.morningharvest.erp.payment.dto.CheckoutResponse;
import com.morningharvest.erp.payment.dto.PaymentTransactionDTO;
import com.morningharvest.erp.payment.entity.PaymentTransaction;
import com.morningharvest.erp.payment.event.PaymentCompletedEvent;
import com.morningharvest.erp.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    /**
     * 結帳付款（更新 PENDING 付款條目為 COMPLETED）
     */
    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {
        log.info("結帳付款, orderId: {}, amountReceived: {}", request.getOrderId(), request.getAmountReceived());

        // 1. 驗證訂單存在
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在: " + request.getOrderId()));

        // 2. 驗證訂單狀態為 PENDING_PAYMENT
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new IllegalArgumentException("訂單狀態不正確，無法結帳（必須為待付款狀態）");
        }

        // 3. 查詢 PENDING 狀態的付款條目
        PaymentTransaction transaction = paymentTransactionRepository
                .findByOrderIdAndStatus(request.getOrderId(), "PENDING")
                .orElseThrow(() -> new ResourceNotFoundException("找不到待付款的付款條目: " + request.getOrderId()));

        // 4. 驗證付款方式
        if (!"CASH".equals(request.getPaymentMethod())) {
            throw new IllegalArgumentException("目前僅支援現金付款");
        }

        // 5. 驗證實收金額必須大於等於應付金額
        if (request.getAmountReceived().compareTo(transaction.getAmount()) < 0) {
            throw new IllegalArgumentException("實收金額不足");
        }

        // 6. 更新付款條目
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setStatus("COMPLETED");
        transaction.setAmountReceived(request.getAmountReceived());
        transaction.setChangeAmount(request.getChangeAmount());
        transaction.setNote(request.getNote());
        transaction.setTransactionTime(LocalDateTime.now());

        PaymentTransaction saved = paymentTransactionRepository.save(transaction);

        log.info("結帳完成, transactionId: {}, orderId: {}", saved.getId(), order.getId());

        // 7. 發布付款完成事件（訂單狀態由事件監聽器更新）
        eventPublisher.publish(
                new PaymentCompletedEvent(saved.getOrderId(), saved.getId(), saved.getAmount()),
                "付款完成"
        );

        // 重新取得訂單（狀態可能已被事件監聽器更新，但因為是異步的，這裡可能還是舊狀態）
        Order updatedOrder = orderRepository.findById(order.getId()).orElse(order);

        return CheckoutResponse.builder()
                .transactionId(saved.getId())
                .orderId(saved.getOrderId())
                .paymentMethod(saved.getPaymentMethod())
                .status(saved.getStatus())
                .amount(saved.getAmount())
                .amountReceived(saved.getAmountReceived())
                .changeAmount(saved.getChangeAmount())
                .transactionTime(saved.getTransactionTime())
                .order(OrderDTO.from(updatedOrder))
                .build();
    }

    /**
     * 查詢訂單的付款記錄列表
     */
    @Transactional(readOnly = true)
    public List<PaymentTransactionDTO> getPaymentsByOrderId(Long orderId) {
        log.debug("查詢付款記錄, orderId: {}", orderId);

        return paymentTransactionRepository.findByOrderIdOrderByIdDesc(orderId)
                .stream()
                .map(PaymentTransactionDTO::from)
                .toList();
    }

    /**
     * 查詢訂單的付款資訊（單筆）
     */
    @Transactional(readOnly = true)
    public PaymentTransactionDTO getPaymentByOrderId(Long orderId) {
        log.debug("查詢付款資訊, orderId: {}", orderId);

        // 優先查詢 PENDING 狀態的付款條目
        return paymentTransactionRepository.findByOrderIdAndStatus(orderId, "PENDING")
                .or(() -> paymentTransactionRepository.findByOrderIdAndStatus(orderId, "COMPLETED"))
                .map(PaymentTransactionDTO::from)
                .orElseThrow(() -> new ResourceNotFoundException("找不到付款資訊: " + orderId));
    }
}
