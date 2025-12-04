package com.morningharvest.erp.invoice.service;

import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.invoice.client.InvoiceServiceClient;
import com.morningharvest.erp.invoice.client.dto.*;
import com.morningharvest.erp.invoice.dto.*;
import com.morningharvest.erp.invoice.entity.Invoice;
import com.morningharvest.erp.invoice.entity.InvoiceAllowance;
import com.morningharvest.erp.invoice.entity.InvoiceItem;
import com.morningharvest.erp.invoice.repository.InvoiceAllowanceRepository;
import com.morningharvest.erp.invoice.repository.InvoiceItemRepository;
import com.morningharvest.erp.invoice.repository.InvoiceRepository;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.entity.SingleOrderItem;
import com.morningharvest.erp.order.repository.OrderItemRepository;
import com.morningharvest.erp.order.repository.OrderRepository;
import com.morningharvest.erp.payment.entity.PaymentTransaction;
import com.morningharvest.erp.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceService 單元測試")
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    @Mock
    private InvoiceAllowanceRepository invoiceAllowanceRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private InvoiceServiceClient invoiceServiceClient;

    @InjectMocks
    private InvoiceService invoiceService;

    private Order testOrder;
    private PaymentTransaction testPaymentTransaction;
    private Invoice testInvoice;
    private Invoice voidedInvoice;
    private InvoiceItem testInvoiceItem;
    private InvoiceAllowance testAllowance;
    private SingleOrderItem testOrderItem;
    private IssueInvoiceRequest issueInvoiceRequest;
    private VoidInvoiceRequest voidInvoiceRequest;
    private CreateAllowanceRequest createAllowanceRequest;

    @BeforeEach
    void setUp() {
        // 測試訂單
        testOrder = Order.builder()
                .id(1L)
                .status("PAID")
                .orderType("DINE_IN")
                .totalAmount(new BigDecimal("105.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 測試付款交易
        testPaymentTransaction = PaymentTransaction.builder()
                .id(1L)
                .orderId(1L)
                .paymentMethod("CASH")
                .status("COMPLETED")
                .amount(new BigDecimal("105.00"))
                .transactionTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 測試發票 (已開立)
        testInvoice = Invoice.builder()
                .id(1L)
                .orderId(1L)
                .paymentTransactionId(1L)
                .invoiceNumber("AA-00000001")
                .invoiceDate(LocalDate.now())
                .invoicePeriod("11312")
                .invoiceType("B2C")
                .issueType("ELECTRONIC")
                .salesAmount(new BigDecimal("100.00"))
                .taxAmount(new BigDecimal("5.00"))
                .totalAmount(new BigDecimal("105.00"))
                .status("ISSUED")
                .isPrinted(false)
                .printCount(0)
                .isVoided(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 已作廢發票
        voidedInvoice = Invoice.builder()
                .id(2L)
                .orderId(2L)
                .paymentTransactionId(2L)
                .invoiceNumber("AA-00000002")
                .invoiceDate(LocalDate.now())
                .invoicePeriod("11312")
                .invoiceType("B2C")
                .issueType("ELECTRONIC")
                .status("VOID")
                .isVoided(true)
                .voidedAt(LocalDateTime.now())
                .voidReason("測試作廢")
                .build();

        // 測試發票明細
        testInvoiceItem = InvoiceItem.builder()
                .id(1L)
                .invoiceId(1L)
                .sequence(1)
                .description("測試商品")
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal("105.00"))
                .amount(new BigDecimal("105.00"))
                .build();

        // 測試折讓
        testAllowance = InvoiceAllowance.builder()
                .id(1L)
                .invoiceId(1L)
                .allowanceNumber("AA-20000001")
                .allowanceDate(LocalDate.now())
                .salesAmount(new BigDecimal("50.00"))
                .taxAmount(new BigDecimal("2.00"))
                .totalAmount(new BigDecimal("52.00"))
                .reason("測試折讓")
                .status("ISSUED")
                .build();

        // 測試訂單項目
        testOrderItem = SingleOrderItem.builder()
                .productId(1L)
                .productName("測試商品")
                .unitPrice(new BigDecimal("105.00"))
                .quantity(1)
                .optionsAmount(BigDecimal.ZERO)
                .build();
        testOrderItem.setId(1L);
        testOrderItem.setOrderId(1L);
        testOrderItem.setSubtotal(new BigDecimal("105.00"));

        // 開立發票請求
        issueInvoiceRequest = IssueInvoiceRequest.builder()
                .orderId(1L)
                .paymentTransactionId(1L)
                .invoiceType("B2C")
                .issueType("ELECTRONIC")
                .carrierType("MOBILE_BARCODE")
                .carrierValue("/ABC1234")
                .isDonated(false)
                .build();

        // 作廢發票請求
        voidInvoiceRequest = VoidInvoiceRequest.builder()
                .invoiceId(1L)
                .reason("顧客退貨")
                .build();

        // 折讓請求
        createAllowanceRequest = CreateAllowanceRequest.builder()
                .invoiceId(1L)
                .amount(new BigDecimal("52.00"))
                .reason("部分退款")
                .build();
    }

    // ========== issueInvoice 測試 ==========

    @Test
    @DisplayName("開立發票 - 成功")
    void issueInvoice_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentTransactionRepository.findById(1L)).thenReturn(Optional.of(testPaymentTransaction));
        when(invoiceRepository.existsByOrderId(1L)).thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            invoice.setId(1L);
            return invoice;
        });
        when(orderItemRepository.findByOrderIdOrderByIdAsc(1L)).thenReturn(List.of(testOrderItem));
        when(invoiceItemRepository.saveAll(anyList())).thenReturn(List.of(testInvoiceItem));
        when(invoiceServiceClient.issueInvoice(any(IssueInvoiceExternalRequest.class)))
                .thenReturn(IssueInvoiceExternalResponse.builder()
                        .success(true)
                        .invoiceNumber("AA-00000001")
                        .invoiceDate(LocalDate.now())
                        .invoicePeriod("11312")
                        .externalId("mock-12345678")
                        .resultCode("SUCCESS")
                        .resultMessage("Mock 開立成功")
                        .build());

        // When
        InvoiceResult result = invoiceService.issueInvoice(issueInvoiceRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getInvoiceId()).isEqualTo(1L);
        assertThat(result.getInvoiceNumber()).isEqualTo("AA-00000001");
        assertThat(result.getStatus()).isEqualTo("ISSUED");

        verify(orderRepository).findById(1L);
        verify(paymentTransactionRepository).findById(1L);
        verify(invoiceRepository).existsByOrderId(1L);
        verify(invoiceRepository, times(2)).save(any(Invoice.class));
        verify(orderItemRepository).findByOrderIdOrderByIdAsc(1L);
        verify(invoiceItemRepository).saveAll(anyList());
        verify(invoiceServiceClient).issueInvoice(any(IssueInvoiceExternalRequest.class));
    }

    @Test
    @DisplayName("開立發票 - 訂單不存在拋出例外")
    void issueInvoice_OrderNotFound_ThrowsException() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        IssueInvoiceRequest request = IssueInvoiceRequest.builder()
                .orderId(999L)
                .paymentTransactionId(1L)
                .build();

        // When & Then
        assertThatThrownBy(() -> invoiceService.issueInvoice(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("訂單不存在");

        verify(orderRepository).findById(999L);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("開立發票 - 付款交易不存在拋出例外")
    void issueInvoice_PaymentNotFound_ThrowsException() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentTransactionRepository.findById(999L)).thenReturn(Optional.empty());

        IssueInvoiceRequest request = IssueInvoiceRequest.builder()
                .orderId(1L)
                .paymentTransactionId(999L)
                .build();

        // When & Then
        assertThatThrownBy(() -> invoiceService.issueInvoice(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("付款交易不存在");

        verify(paymentTransactionRepository).findById(999L);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("開立發票 - 重複開立拋出例外")
    void issueInvoice_DuplicateInvoice_ThrowsException() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentTransactionRepository.findById(1L)).thenReturn(Optional.of(testPaymentTransaction));
        when(invoiceRepository.existsByOrderId(1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> invoiceService.issueInvoice(issueInvoiceRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("該訂單已開立發票");

        verify(invoiceRepository).existsByOrderId(1L);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("開立發票 - 外部服務失敗時狀態為 FAILED")
    void issueInvoice_ExternalServiceFailed_StatusFailed() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentTransactionRepository.findById(1L)).thenReturn(Optional.of(testPaymentTransaction));
        when(invoiceRepository.existsByOrderId(1L)).thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            invoice.setId(1L);
            return invoice;
        });
        when(orderItemRepository.findByOrderIdOrderByIdAsc(1L)).thenReturn(List.of(testOrderItem));
        when(invoiceItemRepository.saveAll(anyList())).thenReturn(List.of(testInvoiceItem));
        when(invoiceServiceClient.issueInvoice(any(IssueInvoiceExternalRequest.class)))
                .thenReturn(IssueInvoiceExternalResponse.builder()
                        .success(false)
                        .resultCode("ERROR")
                        .resultMessage("外部服務錯誤")
                        .build());

        // When
        InvoiceResult result = invoiceService.issueInvoice(issueInvoiceRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getMessage()).isEqualTo("外部服務錯誤");

        verify(invoiceServiceClient).issueInvoice(any(IssueInvoiceExternalRequest.class));
    }

    // ========== voidInvoice 測試 ==========

    @Test
    @DisplayName("作廢發票 - 成功")
    void voidInvoice_Success() {
        // Given
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(invoiceServiceClient.voidInvoice(any(VoidInvoiceExternalRequest.class)))
                .thenReturn(VoidInvoiceExternalResponse.success());
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);

        // When
        InvoiceResult result = invoiceService.voidInvoice(voidInvoiceRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getInvoiceId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("VOID");

        verify(invoiceRepository).findById(1L);
        verify(invoiceServiceClient).voidInvoice(any(VoidInvoiceExternalRequest.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("作廢發票 - 發票不存在拋出例外")
    void voidInvoice_InvoiceNotFound_ThrowsException() {
        // Given
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        VoidInvoiceRequest request = VoidInvoiceRequest.builder()
                .invoiceId(999L)
                .reason("測試")
                .build();

        // When & Then
        assertThatThrownBy(() -> invoiceService.voidInvoice(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("發票不存在");

        verify(invoiceRepository).findById(999L);
        verify(invoiceServiceClient, never()).voidInvoice(any());
    }

    @Test
    @DisplayName("作廢發票 - 非已開立狀態拋出例外")
    void voidInvoice_NotIssuedStatus_ThrowsException() {
        // Given
        when(invoiceRepository.findById(2L)).thenReturn(Optional.of(voidedInvoice));

        VoidInvoiceRequest request = VoidInvoiceRequest.builder()
                .invoiceId(2L)
                .reason("測試")
                .build();

        // When & Then
        assertThatThrownBy(() -> invoiceService.voidInvoice(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只有已開立的發票才能作廢");

        verify(invoiceRepository).findById(2L);
        verify(invoiceServiceClient, never()).voidInvoice(any());
    }

    @Test
    @DisplayName("作廢發票 - 跨月發票拋出例外")
    void voidInvoice_CrossMonth_ThrowsException() {
        // Given
        Invoice lastMonthInvoice = Invoice.builder()
                .id(3L)
                .invoiceNumber("AA-00000003")
                .invoiceDate(LocalDate.now().minusMonths(1))
                .status("ISSUED")
                .build();
        when(invoiceRepository.findById(3L)).thenReturn(Optional.of(lastMonthInvoice));

        VoidInvoiceRequest request = VoidInvoiceRequest.builder()
                .invoiceId(3L)
                .reason("測試")
                .build();

        // When & Then
        assertThatThrownBy(() -> invoiceService.voidInvoice(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只能作廢當月發票");

        verify(invoiceRepository).findById(3L);
        verify(invoiceServiceClient, never()).voidInvoice(any());
    }

    // ========== createAllowance 測試 ==========

    @Test
    @DisplayName("開立折讓 - 成功")
    void createAllowance_Success() {
        // Given
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(invoiceAllowanceRepository.save(any(InvoiceAllowance.class))).thenAnswer(invocation -> {
            InvoiceAllowance allowance = invocation.getArgument(0);
            allowance.setId(1L);
            return allowance;
        });
        when(invoiceServiceClient.createAllowance(any(AllowanceExternalRequest.class)))
                .thenReturn(AllowanceExternalResponse.builder()
                        .success(true)
                        .allowanceNumber("AA-20000001")
                        .allowanceDate(LocalDate.now())
                        .externalId("mock-allowance-123")
                        .resultCode("SUCCESS")
                        .resultMessage("Mock 折讓開立成功")
                        .build());

        // When
        InvoiceAllowanceDTO result = invoiceService.createAllowance(createAllowanceRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getInvoiceId()).isEqualTo(1L);
        assertThat(result.getAllowanceNumber()).isEqualTo("AA-20000001");
        assertThat(result.getStatus()).isEqualTo("ISSUED");

        verify(invoiceRepository).findById(1L);
        verify(invoiceAllowanceRepository, times(2)).save(any(InvoiceAllowance.class));
        verify(invoiceServiceClient).createAllowance(any(AllowanceExternalRequest.class));
    }

    @Test
    @DisplayName("開立折讓 - 發票不存在拋出例外")
    void createAllowance_InvoiceNotFound_ThrowsException() {
        // Given
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        CreateAllowanceRequest request = CreateAllowanceRequest.builder()
                .invoiceId(999L)
                .amount(new BigDecimal("50.00"))
                .reason("測試")
                .build();

        // When & Then
        assertThatThrownBy(() -> invoiceService.createAllowance(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("發票不存在");

        verify(invoiceRepository).findById(999L);
        verify(invoiceAllowanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("開立折讓 - 非已開立狀態拋出例外")
    void createAllowance_NotIssuedStatus_ThrowsException() {
        // Given
        when(invoiceRepository.findById(2L)).thenReturn(Optional.of(voidedInvoice));

        CreateAllowanceRequest request = CreateAllowanceRequest.builder()
                .invoiceId(2L)
                .amount(new BigDecimal("50.00"))
                .reason("測試")
                .build();

        // When & Then
        assertThatThrownBy(() -> invoiceService.createAllowance(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只有已開立的發票才能折讓");

        verify(invoiceRepository).findById(2L);
        verify(invoiceAllowanceRepository, never()).save(any());
    }

    // ========== recordPrint 測試 ==========

    @Test
    @DisplayName("記錄列印 - 成功")
    void recordPrint_Success() {
        // Given
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);
        when(invoiceItemRepository.findByInvoiceIdOrderBySequenceAsc(1L)).thenReturn(List.of(testInvoiceItem));

        // When
        InvoiceDTO result = invoiceService.recordPrint(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(testInvoice.getIsPrinted()).isTrue();
        assertThat(testInvoice.getPrintCount()).isEqualTo(1);

        verify(invoiceRepository).findById(1L);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("記錄列印 - 發票不存在拋出例外")
    void recordPrint_InvoiceNotFound_ThrowsException() {
        // Given
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> invoiceService.recordPrint(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("發票不存在");

        verify(invoiceRepository).findById(999L);
        verify(invoiceRepository, never()).save(any());
    }

    // ========== getInvoiceById 測試 ==========

    @Test
    @DisplayName("查詢發票詳情 - 成功")
    void getInvoiceById_Success() {
        // Given
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(invoiceItemRepository.findByInvoiceIdOrderBySequenceAsc(1L)).thenReturn(List.of(testInvoiceItem));

        // When
        InvoiceDTO result = invoiceService.getInvoiceById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getInvoiceNumber()).isEqualTo("AA-00000001");
        assertThat(result.getItems()).hasSize(1);

        verify(invoiceRepository).findById(1L);
        verify(invoiceItemRepository).findByInvoiceIdOrderBySequenceAsc(1L);
    }

    @Test
    @DisplayName("查詢發票詳情 - 發票不存在拋出例外")
    void getInvoiceById_NotFound_ThrowsException() {
        // Given
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> invoiceService.getInvoiceById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("發票不存在");

        verify(invoiceRepository).findById(999L);
    }

    // ========== getInvoiceByOrderId 測試 ==========

    @Test
    @DisplayName("依訂單查詢發票 - 成功")
    void getInvoiceByOrderId_Success() {
        // Given
        when(invoiceRepository.findByOrderId(1L)).thenReturn(Optional.of(testInvoice));
        when(invoiceItemRepository.findByInvoiceIdOrderBySequenceAsc(1L)).thenReturn(List.of(testInvoiceItem));

        // When
        InvoiceDTO result = invoiceService.getInvoiceByOrderId(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOrderId()).isEqualTo(1L);

        verify(invoiceRepository).findByOrderId(1L);
    }

    @Test
    @DisplayName("依訂單查詢發票 - 發票不存在拋出例外")
    void getInvoiceByOrderId_NotFound_ThrowsException() {
        // Given
        when(invoiceRepository.findByOrderId(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> invoiceService.getInvoiceByOrderId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("找不到該訂單的發票");

        verify(invoiceRepository).findByOrderId(999L);
    }

    // ========== listInvoices 測試 ==========

    @Test
    @DisplayName("查詢發票列表 - 依日期範圍篩選")
    void listInvoices_FilterByDateRange() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Invoice> invoicePage = new PageImpl<>(List.of(testInvoice));

        when(invoiceRepository.findByInvoiceDateBetween(startDate, endDate, pageable)).thenReturn(invoicePage);
        when(invoiceItemRepository.findByInvoiceIdOrderBySequenceAsc(1L)).thenReturn(List.of(testInvoiceItem));

        // When
        Page<InvoiceDTO> result = invoiceService.listInvoices(startDate, endDate, null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getInvoiceNumber()).isEqualTo("AA-00000001");

        verify(invoiceRepository).findByInvoiceDateBetween(startDate, endDate, pageable);
    }

    @Test
    @DisplayName("查詢發票列表 - 依狀態篩選")
    void listInvoices_FilterByStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Invoice> invoicePage = new PageImpl<>(List.of(testInvoice));

        when(invoiceRepository.findByStatus("ISSUED", pageable)).thenReturn(invoicePage);
        when(invoiceItemRepository.findByInvoiceIdOrderBySequenceAsc(1L)).thenReturn(List.of(testInvoiceItem));

        // When
        Page<InvoiceDTO> result = invoiceService.listInvoices(null, null, "ISSUED", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("ISSUED");

        verify(invoiceRepository).findByStatus("ISSUED", pageable);
    }

    @Test
    @DisplayName("查詢發票列表 - 依日期範圍和狀態篩選")
    void listInvoices_FilterByDateRangeAndStatus() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Invoice> invoicePage = new PageImpl<>(List.of(testInvoice));

        when(invoiceRepository.findByInvoiceDateBetweenAndStatus(startDate, endDate, "ISSUED", pageable))
                .thenReturn(invoicePage);
        when(invoiceItemRepository.findByInvoiceIdOrderBySequenceAsc(1L)).thenReturn(List.of(testInvoiceItem));

        // When
        Page<InvoiceDTO> result = invoiceService.listInvoices(startDate, endDate, "ISSUED", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(invoiceRepository).findByInvoiceDateBetweenAndStatus(startDate, endDate, "ISSUED", pageable);
    }

    @Test
    @DisplayName("查詢發票列表 - 無篩選條件")
    void listInvoices_NoFilter() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Invoice> invoicePage = new PageImpl<>(List.of(testInvoice));

        when(invoiceRepository.findAll(pageable)).thenReturn(invoicePage);
        when(invoiceItemRepository.findByInvoiceIdOrderBySequenceAsc(1L)).thenReturn(List.of(testInvoiceItem));

        // When
        Page<InvoiceDTO> result = invoiceService.listInvoices(null, null, null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(invoiceRepository).findAll(pageable);
    }

    // ========== getAllowancesByInvoiceId 測試 ==========

    @Test
    @DisplayName("查詢折讓記錄 - 成功")
    void getAllowancesByInvoiceId_Success() {
        // Given
        when(invoiceAllowanceRepository.findByInvoiceIdOrderByIdDesc(1L)).thenReturn(List.of(testAllowance));

        // When
        List<InvoiceAllowanceDTO> result = invoiceService.getAllowancesByInvoiceId(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAllowanceNumber()).isEqualTo("AA-20000001");
        assertThat(result.get(0).getTotalAmount()).isEqualByComparingTo(new BigDecimal("52.00"));

        verify(invoiceAllowanceRepository).findByInvoiceIdOrderByIdDesc(1L);
    }

    @Test
    @DisplayName("查詢折讓記錄 - 無記錄返回空列表")
    void getAllowancesByInvoiceId_NoRecords_ReturnsEmptyList() {
        // Given
        when(invoiceAllowanceRepository.findByInvoiceIdOrderByIdDesc(999L)).thenReturn(List.of());

        // When
        List<InvoiceAllowanceDTO> result = invoiceService.getAllowancesByInvoiceId(999L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(invoiceAllowanceRepository).findByInvoiceIdOrderByIdDesc(999L);
    }
}
