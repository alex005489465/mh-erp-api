package com.morningharvest.erp.payment.service;

import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.order.dto.OrderDTO;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.repository.OrderRepository;
import com.morningharvest.erp.payment.dto.CheckoutRequest;
import com.morningharvest.erp.payment.dto.CheckoutResponse;
import com.morningharvest.erp.payment.dto.PaymentTransactionDTO;
import com.morningharvest.erp.payment.entity.PaymentTransaction;
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

    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {
        log.info("結帳付款, orderId: {}, amount: {}", request.getOrderId(), request.getAmount());

        // 1. 驗證訂單存在
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在: " + request.getOrderId()));

        // 2. 驗證訂單狀態
        if (!"DRAFT".equals(order.getStatus())) {
            throw new IllegalArgumentException("訂單狀態不正確，無法結帳");
        }

        // 3. 驗證是否已付款
        if (paymentTransactionRepository.existsByOrderIdAndStatus(request.getOrderId(), "COMPLETED")) {
            throw new IllegalArgumentException("訂單已完成付款");
        }

        // 4. 驗證付款方式
        if (!"CASH".equals(request.getPaymentMethod())) {
            throw new IllegalArgumentException("目前僅支援現金付款");
        }

        // 5. 驗證金額（必須等於訂單金額）
        if (request.getAmount().compareTo(order.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("付款金額必須等於訂單金額");
        }

        // 6. 建立付款交易
        PaymentTransaction transaction = PaymentTransaction.builder()
                .orderId(request.getOrderId())
                .paymentMethod(request.getPaymentMethod())
                .status("COMPLETED")
                .amount(request.getAmount())
                .note(request.getNote())
                .transactionTime(LocalDateTime.now())
                .build();

        PaymentTransaction saved = paymentTransactionRepository.save(transaction);

        // 7. 更新訂單狀態
        order.setStatus("COMPLETED");
        Order updatedOrder = orderRepository.save(order);

        log.info("結帳完成, transactionId: {}, orderId: {}", saved.getId(), order.getId());

        return CheckoutResponse.builder()
                .transactionId(saved.getId())
                .orderId(saved.getOrderId())
                .paymentMethod(saved.getPaymentMethod())
                .status(saved.getStatus())
                .amount(saved.getAmount())
                .transactionTime(saved.getTransactionTime())
                .order(OrderDTO.from(updatedOrder))
                .build();
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionDTO> getPaymentsByOrderId(Long orderId) {
        log.debug("查詢付款記錄, orderId: {}", orderId);

        return paymentTransactionRepository.findByOrderIdOrderByIdDesc(orderId)
                .stream()
                .map(PaymentTransactionDTO::from)
                .toList();
    }
}
