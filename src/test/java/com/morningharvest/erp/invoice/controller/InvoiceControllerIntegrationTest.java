package com.morningharvest.erp.invoice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.invoice.dto.CreateAllowanceRequest;
import com.morningharvest.erp.invoice.dto.IssueInvoiceRequest;
import com.morningharvest.erp.invoice.dto.VoidInvoiceRequest;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("InvoiceController 整合測試")
class InvoiceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private InvoiceAllowanceRepository invoiceAllowanceRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    private Order testOrder;
    private PaymentTransaction testPaymentTransaction;
    private Invoice testInvoice;
    private InvoiceItem testInvoiceItem;
    private InvoiceAllowance testAllowance;

    @BeforeEach
    void setUp() {
        // 清空測試資料
        invoiceAllowanceRepository.deleteAll();
        invoiceItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();

        // 建立測試訂單
        testOrder = Order.builder()
                .status("PAID")
                .orderType("DINE_IN")
                .totalAmount(new BigDecimal("105.00"))
                .build();
        testOrder = orderRepository.save(testOrder);

        // 建立測試訂單項目
        SingleOrderItem orderItem = SingleOrderItem.builder()
                .productId(1L)
                .productName("測試商品")
                .unitPrice(new BigDecimal("105.00"))
                .quantity(1)
                .optionsAmount(BigDecimal.ZERO)
                .build();
        orderItem.setOrderId(testOrder.getId());
        orderItem.setSubtotal(new BigDecimal("105.00"));
        orderItemRepository.save(orderItem);

        // 建立測試付款交易
        testPaymentTransaction = PaymentTransaction.builder()
                .orderId(testOrder.getId())
                .paymentMethod("CASH")
                .status("COMPLETED")
                .amount(new BigDecimal("105.00"))
                .amountReceived(new BigDecimal("200.00"))
                .changeAmount(new BigDecimal("95.00"))
                .transactionTime(LocalDateTime.now())
                .build();
        testPaymentTransaction = paymentTransactionRepository.save(testPaymentTransaction);

        // 建立測試發票
        testInvoice = Invoice.builder()
                .orderId(testOrder.getId())
                .paymentTransactionId(testPaymentTransaction.getId())
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
                .isDonated(false)
                .build();
        testInvoice = invoiceRepository.save(testInvoice);

        // 建立測試發票明細
        testInvoiceItem = InvoiceItem.builder()
                .invoiceId(testInvoice.getId())
                .sequence(1)
                .description("測試商品")
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal("105.00"))
                .amount(new BigDecimal("105.00"))
                .build();
        testInvoiceItem = invoiceItemRepository.save(testInvoiceItem);

        // 建立測試折讓
        testAllowance = InvoiceAllowance.builder()
                .invoiceId(testInvoice.getId())
                .allowanceNumber("AA-20000001")
                .allowanceDate(LocalDate.now())
                .salesAmount(new BigDecimal("48.00"))
                .taxAmount(new BigDecimal("2.00"))
                .totalAmount(new BigDecimal("50.00"))
                .reason("測試折讓")
                .status("ISSUED")
                .build();
        testAllowance = invoiceAllowanceRepository.save(testAllowance);
    }

    // ========== POST /api/invoices/issue 測試 ==========

    @Test
    @DisplayName("開立發票 - 成功")
    void issueInvoice_Success() throws Exception {
        // Given - 建立新訂單 (不使用已有發票的訂單)
        Order newOrder = Order.builder()
                .status("PAID")
                .orderType("TAKEOUT")
                .totalAmount(new BigDecimal("210.00"))
                .build();
        newOrder = orderRepository.save(newOrder);

        SingleOrderItem newOrderItem = SingleOrderItem.builder()
                .productId(2L)
                .productName("新商品")
                .unitPrice(new BigDecimal("210.00"))
                .quantity(1)
                .optionsAmount(BigDecimal.ZERO)
                .build();
        newOrderItem.setOrderId(newOrder.getId());
        newOrderItem.setSubtotal(new BigDecimal("210.00"));
        orderItemRepository.save(newOrderItem);

        PaymentTransaction newPayment = PaymentTransaction.builder()
                .orderId(newOrder.getId())
                .paymentMethod("CASH")
                .status("COMPLETED")
                .amount(new BigDecimal("210.00"))
                .transactionTime(LocalDateTime.now())
                .build();
        newPayment = paymentTransactionRepository.save(newPayment);

        IssueInvoiceRequest request = IssueInvoiceRequest.builder()
                .orderId(newOrder.getId())
                .paymentTransactionId(newPayment.getId())
                .invoiceType("B2C")
                .issueType("ELECTRONIC")
                .carrierType("MOBILE_BARCODE")
                .carrierValue("/ABC1234")
                .isDonated(false)
                .build();

        // When & Then
        mockMvc.perform(post("/api/invoices/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("開立發票 - 重複開立回傳錯誤")
    void issueInvoice_DuplicateInvoice_ReturnsError() throws Exception {
        // Given - 使用已有發票的訂單
        IssueInvoiceRequest request = IssueInvoiceRequest.builder()
                .orderId(testOrder.getId())
                .paymentTransactionId(testPaymentTransaction.getId())
                .invoiceType("B2C")
                .issueType("ELECTRONIC")
                .build();

        // When & Then
        mockMvc.perform(post("/api/invoices/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("已開立發票")));
    }

    // ========== POST /api/invoices/void 測試 ==========

    @Test
    @DisplayName("作廢發票 - 成功")
    void voidInvoice_Success() throws Exception {
        // Given
        VoidInvoiceRequest request = VoidInvoiceRequest.builder()
                .invoiceId(testInvoice.getId())
                .reason("顧客要求取消")
                .build();

        // When & Then
        mockMvc.perform(post("/api/invoices/void")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("VOID"));
    }

    // ========== POST /api/invoices/allowance 測試 ==========

    @Test
    @DisplayName("開立折讓 - 成功")
    void createAllowance_Success() throws Exception {
        // Given
        CreateAllowanceRequest request = CreateAllowanceRequest.builder()
                .invoiceId(testInvoice.getId())
                .amount(new BigDecimal("30.00"))
                .reason("部分退款")
                .build();

        // When & Then
        mockMvc.perform(post("/api/invoices/allowance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.allowanceNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.totalAmount").value(30.00));
    }

    // ========== POST /api/invoices/print 測試 ==========

    @Test
    @DisplayName("記錄列印 - 成功")
    void recordPrint_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/invoices/print")
                        .param("id", testInvoice.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isPrinted").value(true))
                .andExpect(jsonPath("$.data.printCount").value(1));
    }

    // ========== GET /api/invoices/detail 測試 ==========

    @Test
    @DisplayName("查詢發票詳情 - 成功")
    void getInvoiceById_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/invoices/detail")
                        .param("id", testInvoice.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testInvoice.getId()))
                .andExpect(jsonPath("$.data.invoiceNumber").value("AA-00000001"))
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    @Test
    @DisplayName("查詢發票詳情 - 發票不存在")
    void getInvoiceById_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/invoices/detail")
                        .param("id", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("發票不存在")));
    }

    // ========== GET /api/invoices/by-order 測試 ==========

    @Test
    @DisplayName("依訂單查詢發票 - 成功")
    void getInvoiceByOrderId_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/invoices/by-order")
                        .param("orderId", testOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(testOrder.getId()))
                .andExpect(jsonPath("$.data.invoiceNumber").value("AA-00000001"));
    }

    // ========== GET /api/invoices/list 測試 ==========

    @Test
    @DisplayName("查詢發票列表 - 分頁查詢")
    void listInvoices_Pagination() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/invoices/list")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @DisplayName("查詢發票列表 - 依日期範圍篩選")
    void listInvoices_FilterByDateRange() throws Exception {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);

        // When & Then
        mockMvc.perform(get("/api/invoices/list")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].invoiceNumber").value("AA-00000001"));
    }

    // ========== GET /api/invoices/allowances 測試 ==========

    @Test
    @DisplayName("查詢折讓記錄 - 成功")
    void getAllowancesByInvoiceId_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/invoices/allowances")
                        .param("invoiceId", testInvoice.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].allowanceNumber").value("AA-20000001"))
                .andExpect(jsonPath("$.data[0].totalAmount").value(50.00));
    }
}
