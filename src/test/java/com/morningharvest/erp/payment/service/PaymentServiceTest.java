package com.morningharvest.erp.payment.service;

import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.repository.OrderRepository;
import com.morningharvest.erp.payment.dto.CheckoutRequest;
import com.morningharvest.erp.payment.dto.CheckoutResponse;
import com.morningharvest.erp.payment.dto.PaymentTransactionDTO;
import com.morningharvest.erp.payment.entity.PaymentTransaction;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 單元測試")
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Order draftOrder;
    private Order completedOrder;
    private PaymentTransaction testTransaction;

    @BeforeEach
    void setUp() {
        draftOrder = Order.builder()
                .id(1L)
                .status("DRAFT")
                .orderType("DINE_IN")
                .totalAmount(new BigDecimal("150.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        completedOrder = Order.builder()
                .id(2L)
                .status("COMPLETED")
                .orderType("TAKEOUT")
                .totalAmount(new BigDecimal("200.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testTransaction = PaymentTransaction.builder()
                .id(1L)
                .orderId(1L)
                .paymentMethod("CASH")
                .status("COMPLETED")
                .amount(new BigDecimal("150.00"))
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
                .amount(new BigDecimal("150.00"))
                .note("現金付款")
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(draftOrder));
        when(paymentTransactionRepository.existsByOrderIdAndStatus(1L, "COMPLETED")).thenReturn(false);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction t = invocation.getArgument(0);
            t.setId(1L);
            t.setCreatedAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
            return t;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CheckoutResponse result = paymentService.checkout(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTransactionId()).isEqualTo(1L);
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getPaymentMethod()).isEqualTo("CASH");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.getOrder()).isNotNull();
        assertThat(result.getOrder().getStatus()).isEqualTo("COMPLETED");

        verify(orderRepository).findById(1L);
        verify(paymentTransactionRepository).existsByOrderIdAndStatus(1L, "COMPLETED");
        verify(paymentTransactionRepository).save(any(PaymentTransaction.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("結帳 - 訂單不存在拋出例外")
    void checkout_OrderNotFound_ThrowsException() {
        // Given
        CheckoutRequest request = CheckoutRequest.builder()
                .orderId(999L)
                .paymentMethod("CASH")
                .amount(new BigDecimal("100.00"))
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
    @DisplayName("結帳 - 訂單狀態非 DRAFT 拋出例外")
    void checkout_OrderNotDraft_ThrowsException() {
        // Given
        CheckoutRequest request = CheckoutRequest.builder()
                .orderId(2L)
                .paymentMethod("CASH")
                .amount(new BigDecimal("200.00"))
                .build();

        when(orderRepository.findById(2L)).thenReturn(Optional.of(completedOrder));

        // When & Then
        assertThatThrownBy(() -> paymentService.checkout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("訂單狀態不正確");

        verify(orderRepository).findById(2L);
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("結帳 - 訂單已付款拋出例外")
    void checkout_AlreadyPaid_ThrowsException() {
        // Given
        CheckoutRequest request = CheckoutRequest.builder()
                .orderId(1L)
                .paymentMethod("CASH")
                .amount(new BigDecimal("150.00"))
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(draftOrder));
        when(paymentTransactionRepository.existsByOrderIdAndStatus(1L, "COMPLETED")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> paymentService.checkout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("訂單已完成付款");

        verify(paymentTransactionRepository).existsByOrderIdAndStatus(1L, "COMPLETED");
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("結帳 - 不支援的付款方式拋出例外")
    void checkout_UnsupportedPaymentMethod_ThrowsException() {
        // Given
        CheckoutRequest request = CheckoutRequest.builder()
                .orderId(1L)
                .paymentMethod("CREDIT_CARD")
                .amount(new BigDecimal("150.00"))
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(draftOrder));
        when(paymentTransactionRepository.existsByOrderIdAndStatus(1L, "COMPLETED")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> paymentService.checkout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("目前僅支援現金付款");

        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("結帳 - 付款金額不符拋出例外（金額不足）")
    void checkout_InsufficientAmount_ThrowsException() {
        // Given
        CheckoutRequest request = CheckoutRequest.builder()
                .orderId(1L)
                .paymentMethod("CASH")
                .amount(new BigDecimal("100.00")) // 不足 150 元
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(draftOrder));
        when(paymentTransactionRepository.existsByOrderIdAndStatus(1L, "COMPLETED")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> paymentService.checkout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("付款金額必須等於訂單金額");

        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("結帳 - 付款金額不符拋出例外（金額超過）")
    void checkout_OverpayAmount_ThrowsException() {
        // Given
        CheckoutRequest request = CheckoutRequest.builder()
                .orderId(1L)
                .paymentMethod("CASH")
                .amount(new BigDecimal("200.00")) // 超過 150 元
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(draftOrder));
        when(paymentTransactionRepository.existsByOrderIdAndStatus(1L, "COMPLETED")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> paymentService.checkout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("付款金額必須等於訂單金額");

        verify(paymentTransactionRepository, never()).save(any());
    }

    // ========== getPaymentsByOrderId 測試 ==========

    @Test
    @DisplayName("查詢付款記錄 - 成功")
    void getPaymentsByOrderId_Success() {
        // Given
        when(paymentTransactionRepository.findByOrderIdOrderByIdDesc(1L))
                .thenReturn(List.of(testTransaction));

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

    @Test
    @DisplayName("查詢付款記錄 - 多筆記錄")
    void getPaymentsByOrderId_MultipleRecords() {
        // Given
        PaymentTransaction transaction1 = PaymentTransaction.builder()
                .id(1L)
                .orderId(1L)
                .paymentMethod("CASH")
                .status("COMPLETED")
                .amount(new BigDecimal("100.00"))
                .transactionTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PaymentTransaction transaction2 = PaymentTransaction.builder()
                .id(2L)
                .orderId(1L)
                .paymentMethod("CASH")
                .status("COMPLETED")
                .amount(new BigDecimal("50.00"))
                .transactionTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentTransactionRepository.findByOrderIdOrderByIdDesc(1L))
                .thenReturn(List.of(transaction2, transaction1)); // 依 ID 降序

        // When
        List<PaymentTransactionDTO> result = paymentService.getPaymentsByOrderId(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(2L); // 最新的在前
        assertThat(result.get(1).getId()).isEqualTo(1L);

        verify(paymentTransactionRepository).findByOrderIdOrderByIdDesc(1L);
    }
}
