package com.morningharvest.erp.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.invoice.entity.Invoice;
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
import com.morningharvest.erp.pos.dto.CreateAllowanceByOrderRequest;
import com.morningharvest.erp.pos.dto.VoidInvoiceByOrderRequest;
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
@DisplayName("PosInvoiceController 整合測試")
class PosInvoiceControllerIntegrationTest {

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
    private Invoice testInvoice;

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
        PaymentTransaction testPaymentTransaction = PaymentTransaction.builder()
                .orderId(testOrder.getId())
                .paymentMethod("CASH")
                .status("COMPLETED")
                .amount(new BigDecimal("105.00"))
                .amountReceived(new BigDecimal("200.00"))
                .changeAmount(new BigDecimal("95.00"))
                .transactionTime(LocalDateTime.now())
                .build();
        paymentTransactionRepository.save(testPaymentTransaction);

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
        InvoiceItem testInvoiceItem = InvoiceItem.builder()
                .invoiceId(testInvoice.getId())
                .sequence(1)
                .description("測試商品")
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal("105.00"))
                .amount(new BigDecimal("105.00"))
                .build();
        invoiceItemRepository.save(testInvoiceItem);
    }

    // ========== GET /api/pos/invoices/detail 測試 ==========

    @Test
    @DisplayName("依訂單查詢發票 - 成功")
    void getInvoiceByOrderId_Success() throws Exception {
        mockMvc.perform(get("/api/pos/invoices/detail")
                        .param("orderId", testOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(testOrder.getId()))
                .andExpect(jsonPath("$.data.invoiceNumber").value("AA-00000001"))
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    @Test
    @DisplayName("依訂單查詢發票 - 訂單無發票返回錯誤")
    void getInvoiceByOrderId_NotFound() throws Exception {
        mockMvc.perform(get("/api/pos/invoices/detail")
                        .param("orderId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("找不到該訂單的發票")));
    }

    // ========== POST /api/pos/invoices/print 測試 ==========

    @Test
    @DisplayName("記錄列印 - 成功")
    void recordPrint_Success() throws Exception {
        mockMvc.perform(post("/api/pos/invoices/print")
                        .param("orderId", testOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isPrinted").value(true))
                .andExpect(jsonPath("$.data.printCount").value(1))
                .andExpect(jsonPath("$.data.lastPrintedAt").isNotEmpty());
    }

    @Test
    @DisplayName("記錄列印 - 訂單無發票返回錯誤")
    void recordPrint_NotFound() throws Exception {
        mockMvc.perform(post("/api/pos/invoices/print")
                        .param("orderId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("找不到該訂單的發票")));
    }

    // ========== POST /api/pos/invoices/void 測試 ==========

    @Test
    @DisplayName("作廢發票 - 成功")
    void voidInvoice_Success() throws Exception {
        VoidInvoiceByOrderRequest request = VoidInvoiceByOrderRequest.builder()
                .orderId(testOrder.getId())
                .reason("POS 取消訂單")
                .build();

        mockMvc.perform(post("/api/pos/invoices/void")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("VOID"))
                .andExpect(jsonPath("$.data.invoiceNumber").value("AA-00000001"));
    }

    @Test
    @DisplayName("作廢發票 - 訂單無發票返回錯誤")
    void voidInvoice_NotFound() throws Exception {
        VoidInvoiceByOrderRequest request = VoidInvoiceByOrderRequest.builder()
                .orderId(999999L)
                .reason("測試")
                .build();

        mockMvc.perform(post("/api/pos/invoices/void")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("找不到該訂單的發票")));
    }

    // ========== POST /api/pos/invoices/allowance 測試 ==========

    @Test
    @DisplayName("開立折讓 - 成功")
    void createAllowance_Success() throws Exception {
        CreateAllowanceByOrderRequest request = CreateAllowanceByOrderRequest.builder()
                .orderId(testOrder.getId())
                .amount(new BigDecimal("30.00"))
                .reason("POS 部分退款")
                .build();

        mockMvc.perform(post("/api/pos/invoices/allowance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.allowanceNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.totalAmount").value(30.00));
    }

    @Test
    @DisplayName("開立折讓 - 訂單無發票返回錯誤")
    void createAllowance_NotFound() throws Exception {
        CreateAllowanceByOrderRequest request = CreateAllowanceByOrderRequest.builder()
                .orderId(999999L)
                .amount(new BigDecimal("30.00"))
                .reason("測試")
                .build();

        mockMvc.perform(post("/api/pos/invoices/allowance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("找不到該訂單的發票")));
    }
}
