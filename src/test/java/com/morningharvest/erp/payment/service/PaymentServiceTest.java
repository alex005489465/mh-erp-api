package com.morningharvest.erp.payment.service;

import com.morningharvest.erp.common.event.EventPublisher;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.repository.OrderRepository;
import com.morningharvest.erp.payment.dto.CheckoutRequest;
import com.morningharvest.erp.payment.dto.CheckoutResponse;
import com.morningharvest.erp.payment.dto.PaymentTransactionDTO;
import com.morningharvest.erp.payment.entity.PaymentTransaction;
import com.morningharvest.erp.payment.event.PaymentCompletedEvent;
import com.morningharvest.erp.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 單元測試")
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private Order pendingPaymentOrder;
    private Order paidOrder;
    private PaymentTransaction pendingTransaction;
    private PaymentTransaction completedTransaction;

    @BeforeEach
    void setUp() {
        pendingPaymentOrder = Order.builder()
                .id(1L)
                .status("PENDING_PAYMENT")
                .orderType("DINE_IN")
                .totalAmount(new BigDecimal("150.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        paidOrder = Order.builder()
                .id(2L)
                .status("PAID")
                .orderType("TAKEOUT")
                .totalAmount(new BigDecimal("200.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        pendingTransaction = PaymentTransaction.builder()
                .id(1L)
                .orderId(1L)
                .paymentMethod("CASH")
                .status("PENDING")
                .amount(new BigDecimal("150.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        completedTransaction = PaymentTransaction.builder()
                .id(1L)
                .orderId(1L)
                .paymentMethod("CASH")
                .status("COMPLETED")
                .amount(new BigDecimal("150.00"))
                .amountReceived(new BigDecimal("200.00"))
                .changeAmount(new BigDecimal("50.00"))
                .note("測試付款")
                .transactionTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ========== checkout 測試 ==========

    @Test
    @DisplayName("結帳 - 成功")
    void checkout_Success() {
        // Given
        CheckoutRequest request = CheckoutRequest.builder()
                .orderId(1L)
                .paymentMethod("CASH")
                .amountReceived(new BigDecimal("200.00"))
                .changeAmount(new BigDecimal("50.00"))
                .note("現金付款")
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingPaymentOrder));
        when(paymentTransactionRepository.findByOrderIdAndStatus(1L, "PENDING"))
                .thenReturn(Optional.of(pendingTransaction));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction t = invocation.getArgument(0);
            t.setTransactionTime(LocalDateTime.now());
            return t;
        });

        // When
        CheckoutResponse result = paymentService.checkout(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTransactionId()).isEqualTo(1L);
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getPaymentMethod()).isEqualTo("CASH");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.getAmountReceived()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(result.getChangeAmount()).isEqualByComparingTo(new BigDecimal("50.00"));

        verify(orderRepository, times(2)).findById(1L); // 一次驗證，一次取得更新後的訂單
        verify(paymentTransactionRepository).findByOrderIdAndStatus(1L, "PENDING");
        verify(paymentTransactionRepository).save(any(PaymentTransaction.class));
        verify(eventPublisher).publish(any(PaymentCompletedEvent.class), eq("付款完成"));
    }

    @Test
    @DisplayName("結帳 - 訂單不存在拋出例外")
    void checkout_OrderNotFound_ThrowsException() {
        // Given
        CheckoutRequest request = CheckoutRequest.builder()
                .orderId(999L)
                .paymentMethod("CASH")
                .amountReceived(new BigDecimal("100.00"))
                .changeAmount(BigDecimal.ZERO)
                .build();

        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.checkout(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("訂單不存在");

        verify(orderRepository).findById(999L);
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("結帳 - 訂單狀態非 PENDING_PAYMENT 拋出例外")
    void checkout_OrderNotPendingPayment_ThrowsException() {
        // Given
        CheckoutRequest request = CheckoutRequest.builder()
                .orderId(2L)
                .paymentMethod("CASH")
                .amountReceived(new BigDecimal("200.00"))
                .changeAmount(BigDecimal.ZERO)
                .build();

        when(orderRepository.findById(2L)).thenReturn(Optional.of(paidOrder));

        // When & Then
        assertThatThrownBy(() -> paymentService.checkout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("訂單狀態不正確");

        verify(orderRepository).findById(2L);
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("結帳 - 找不到待付款條目拋出例外")
    void checkout_NoPendingTransaction_ThrowsException() {
        // Given
        CheckoutRequest request = CheckoutRequest.builder()
                .orderId(1L)
                .paymentMethod("CASH")
                .amountReceived(new BigDecimal("150.00"))
                .changeAmount(BigDecimal.ZERO)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingPaymentOrder));
        when(paymentTransactionRepository.findByOrderIdAndStatus(1L, "PENDING"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.checkout(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("找不到待付款的付款條目");

        verify(paymentTransactionRepository).findByOrderIdAndStatus(1L, "PENDING");
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("結帳 - 不支援的付款方式拋出例外")
    void checkout_UnsupportedPaymentMethod_ThrowsException() {
        // Given
        CheckoutRequest request = CheckoutRequest.builder()
                .orderId(1L)
                .paymentMethod("CREDIT_CARD")
                .amountReceived(new BigDecimal("150.00"))
                .changeAmount(BigDecimal.ZERO)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingPaymentOrder));
        when(paymentTransactionRepository.findByOrderIdAndStatus(1L, "PENDING"))
                .thenReturn(Optional.of(pendingTransaction));

        // When & Then
        assertThatThrownBy(() -> paymentService.checkout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("目前僅支援現金付款");

        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("結帳 - 實收金額不足拋出例外")
    void checkout_InsufficientAmount_ThrowsException() {
        // Given
        CheckoutRequest request = CheckoutRequest.builder()
                .orderId(1L)
                .paymentMethod("CASH")
                .amountReceived(new BigDecimal("100.00")) // 不足 150 元
                .changeAmount(BigDecimal.ZERO)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingPaymentOrder));
        when(paymentTransactionRepository.findByOrderIdAndStatus(1L, "PENDING"))
                .thenReturn(Optional.of(pendingTransaction));

        // When & Then
        assertThatThrownBy(() -> paymentService.checkout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("實收金額不足");

        verify(paymentTransactionRepository, never()).save(any());
    }

    // ========== getPaymentsByOrderId 測試 ==========

    @Test
    @DisplayName("查詢付款記錄 - 成功")
    void getPaymentsByOrderId_Success() {
        // Given
        when(paymentTransactionRepository.findByOrderIdOrderByIdDesc(1L))
                .thenReturn(List.of(completedTransaction));

        // When
        List<PaymentTransactionDTO> result = paymentService.getPaymentsByOrderId(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getOrderId()).isEqualTo(1L);
        assertThat(result.get(0).getPaymentMethod()).isEqualTo("CASH");
        assertThat(result.get(0).getStatus()).isEqualTo("COMPLETED");
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.get(0).getAmountReceived()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(result.get(0).getChangeAmount()).isEqualByComparingTo(new BigDecimal("50.00"));

        verify(paymentTransactionRepository).findByOrderIdOrderByIdDesc(1L);
    }

    @Test
    @DisplayName("查詢付款記錄 - 無記錄返回空列表")
    void getPaymentsByOrderId_NoRecords_ReturnsEmptyList() {
        // Given
        when(paymentTransactionRepository.findByOrderIdOrderByIdDesc(999L))
                .thenReturn(List.of());

        // When
        List<PaymentTransactionDTO> result = paymentService.getPaymentsByOrderId(999L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(paymentTransactionRepository).findByOrderIdOrderByIdDesc(999L);
    }

    // ========== getPaymentByOrderId 測試 ==========

    @Test
    @DisplayName("查詢付款資訊 - 成功（PENDING 狀態）")
    void getPaymentByOrderId_Pending_Success() {
        // Given
        when(paymentTransactionRepository.findByOrderIdAndStatus(1L, "PENDING"))
                .thenReturn(Optional.of(pendingTransaction));

        // When
        PaymentTransactionDTO result = paymentService.getPaymentByOrderId(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));

        verify(paymentTransactionRepository).findByOrderIdAndStatus(1L, "PENDING");
    }

    @Test
    @DisplayName("查詢付款資訊 - 成功（COMPLETED 狀態）")
    void getPaymentByOrderId_Completed_Success() {
        // Given
        when(paymentTransactionRepository.findByOrderIdAndStatus(1L, "PENDING"))
                .thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByOrderIdAndStatus(1L, "COMPLETED"))
                .thenReturn(Optional.of(completedTransaction));

        // When
        PaymentTransactionDTO result = paymentService.getPaymentByOrderId(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");

        verify(paymentTransactionRepository).findByOrderIdAndStatus(1L, "PENDING");
        verify(paymentTransactionRepository).findByOrderIdAndStatus(1L, "COMPLETED");
    }

    @Test
    @DisplayName("查詢付款資訊 - 找不到拋出例外")
    void getPaymentByOrderId_NotFound_ThrowsException() {
        // Given
        when(paymentTransactionRepository.findByOrderIdAndStatus(999L, "PENDING"))
                .thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByOrderIdAndStatus(999L, "COMPLETED"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getPaymentByOrderId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("找不到付款資訊");

        verify(paymentTransactionRepository).findByOrderIdAndStatus(999L, "PENDING");
        verify(paymentTransactionRepository).findByOrderIdAndStatus(999L, "COMPLETED");
    }
}
