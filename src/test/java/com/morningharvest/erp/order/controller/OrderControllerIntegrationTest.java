package com.morningharvest.erp.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.order.dto.*;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.entity.OrderItem;
import com.morningharvest.erp.order.entity.SingleOrderItem;
import com.morningharvest.erp.order.repository.OrderItemRepository;
import com.morningharvest.erp.order.repository.OrderRepository;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.entity.ProductCategory;
import com.morningharvest.erp.product.entity.ProductOptionGroup;
import com.morningharvest.erp.product.entity.ProductOptionValue;
import com.morningharvest.erp.payment.entity.PaymentTransaction;
import com.morningharvest.erp.payment.repository.PaymentTransactionRepository;
import com.morningharvest.erp.product.repository.ProductCategoryRepository;
import com.morningharvest.erp.product.repository.ProductOptionGroupRepository;
import com.morningharvest.erp.product.repository.ProductOptionValueRepository;
import com.morningharvest.erp.product.repository.ProductRepository;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("OrderController 整合測試")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private ProductOptionGroupRepository optionGroupRepository;

    @Autowired
    private ProductOptionValueRepository optionValueRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    private Order testOrder;
    private Order completedOrder;
    private Order paidOrder;
    private Order cancelledOrder;
    private PaymentTransaction paidOrderPayment;
    private Product testProduct;
    private Product inactiveProduct;
    private ProductCategory testCategory;
    private ProductOptionGroup testOptionGroup;
    private ProductOptionValue testOptionValue1;
    private ProductOptionValue testOptionValue2;

    @BeforeEach
    void setUp() {
        paymentTransactionRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        optionValueRepository.deleteAll();
        optionGroupRepository.deleteAll();

        // 建立測試分類
        testCategory = ProductCategory.builder()
                .name("漢堡類")
                .isActive(true)
                .sortOrder(1)
                .build();
        testCategory = productCategoryRepository.save(testCategory);

        // 建立測試商品 (活躍)
        testProduct = Product.builder()
                .name("招牌漢堡")
                .price(new BigDecimal("59.00"))
                .categoryId(testCategory.getId())
                .isActive(true)
                .sortOrder(1)
                .build();
        testProduct = productRepository.save(testProduct);

        // 建立停售商品
        inactiveProduct = Product.builder()
                .name("停售商品")
                .price(new BigDecimal("49.00"))
                .categoryId(testCategory.getId())
                .isActive(false)
                .sortOrder(2)
                .build();
        inactiveProduct = productRepository.save(inactiveProduct);

        // 建立測試訂單 (草稿)
        testOrder = Order.builder()
                .status("DRAFT")
                .orderType("DINE_IN")
                .totalAmount(BigDecimal.ZERO)
                .note("測試訂單")
                .build();
        testOrder = orderRepository.save(testOrder);

        // 建立已完成訂單
        completedOrder = Order.builder()
                .status("COMPLETED")
                .orderType("TAKEOUT")
                .totalAmount(new BigDecimal("118.00"))
                .build();
        completedOrder = orderRepository.save(completedOrder);

        // 建立已付款訂單 (供 completeOrder 測試使用)
        paidOrder = Order.builder()
                .status("PAID")
                .orderType("DINE_IN")
                .totalAmount(new BigDecimal("59.00"))
                .build();
        paidOrder = orderRepository.save(paidOrder);

        // 建立已付款訂單的付款記錄 (供 cancelOrder 測試使用)
        paidOrderPayment = PaymentTransaction.builder()
                .orderId(paidOrder.getId())
                .transactionType("PAYMENT")
                .paymentMethod("CASH")
                .status("COMPLETED")
                .amount(new BigDecimal("59.00"))
                .amountReceived(new BigDecimal("100.00"))
                .changeAmount(new BigDecimal("41.00"))
                .transactionTime(LocalDateTime.now())
                .build();
        paidOrderPayment = paymentTransactionRepository.save(paidOrderPayment);

        // 建立已取消訂單 (供重複取消測試使用)
        cancelledOrder = Order.builder()
                .status("CANCELLED")
                .orderType("DINE_IN")
                .totalAmount(new BigDecimal("59.00"))
                .isCancelled(true)
                .cancelledAt(LocalDateTime.now())
                .cancelReason("客戶取消")
                .build();
        cancelledOrder = orderRepository.save(cancelledOrder);

        // 建立產品選項群組
        testOptionGroup = ProductOptionGroup.builder()
                .productId(testProduct.getId())
                .name("加料")
                .minSelections(0)
                .maxSelections(3)
                .isActive(true)
                .sortOrder(1)
                .build();
        testOptionGroup = optionGroupRepository.save(testOptionGroup);

        // 建立選項值 1 (加起司 +10)
        testOptionValue1 = ProductOptionValue.builder()
                .groupId(testOptionGroup.getId())
                .name("加起司")
                .priceAdjustment(new BigDecimal("10.00"))
                .isActive(true)
                .sortOrder(1)
                .build();
        testOptionValue1 = optionValueRepository.save(testOptionValue1);

        // 建立選項值 2 (加蛋 +15)
        testOptionValue2 = ProductOptionValue.builder()
                .groupId(testOptionGroup.getId())
                .name("加蛋")
                .priceAdjustment(new BigDecimal("15.00"))
                .isActive(true)
                .sortOrder(2)
                .build();
        testOptionValue2 = optionValueRepository.save(testOptionValue2);
    }

    // ========== 建立訂單 POST /api/orders/create ==========

    @Test
    @DisplayName("POST /api/orders/create - 建立空訂單成功")
    void createOrder_Empty_Success() throws Exception {
        mockMvc.perform(post("/api/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.orderType").value("DINE_IN"))
                .andExpect(jsonPath("$.data.totalAmount").value(0));
    }

    @Test
    @DisplayName("POST /api/orders/create - 建立含項目訂單成功")
    void createOrder_WithItems_Success() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .orderType("TAKEOUT")
                .note("外帶訂單")
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(testProduct.getId())
                                .quantity(2)
                                .note("不要酸黃瓜")
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderType").value("TAKEOUT"))
                .andExpect(jsonPath("$.data.note").value("外帶訂單"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].productName").value("招牌漢堡"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].subtotal").value(118.00)) // 59 * 2
                .andExpect(jsonPath("$.data.totalAmount").value(118.00));
    }

    @Test
    @DisplayName("POST /api/orders/create - 含選項成功")
    void createOrder_WithOptions_Success() throws Exception {
        List<OrderItemOptionDTO> options = List.of(
                OrderItemOptionDTO.builder()
                        .groupName("加料")
                        .valueName("加起司")
                        .priceAdjustment(new BigDecimal("10.00"))
                        .build()
        );

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(testProduct.getId())
                                .quantity(1)
                                .options(options)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.items[0].optionsAmount").value(10.00))
                .andExpect(jsonPath("$.data.items[0].subtotal").value(69.00)) // (59 + 10) * 1
                .andExpect(jsonPath("$.data.totalAmount").value(69.00));
    }

    @Test
    @DisplayName("POST /api/orders/create - 無效類型 (code=2001)")
    void createOrder_InvalidOrderType_ValidationError() throws Exception {
        String invalidRequest = "{ \"orderType\": \"INVALID_TYPE\" }";

        mockMvc.perform(post("/api/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.orderType").exists());
    }

    @Test
    @DisplayName("POST /api/orders/create - 商品不存在 (code=3001)")
    void createOrder_ProductNotFound() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(99999L)
                                .quantity(1)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("商品不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/orders/create - 商品已下架 (code=2002)")
    void createOrder_ProductInactive() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(inactiveProduct.getId())
                                .quantity(1)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("商品已下架: 停售商品"));
    }

    // ========== 更新訂單 POST /api/orders/update ==========

    @Test
    @DisplayName("POST /api/orders/update - 更新訂單成功")
    void updateOrder_Success() throws Exception {
        UpdateOrderRequest request = UpdateOrderRequest.builder()
                .orderType("TAKEOUT")
                .note("改外帶")
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(testProduct.getId())
                                .quantity(3)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/orders/update")
                        .param("id", testOrder.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderType").value("TAKEOUT"))
                .andExpect(jsonPath("$.data.note").value("改外帶"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3))
                .andExpect(jsonPath("$.data.totalAmount").value(177.00)); // 59 * 3
    }

    @Test
    @DisplayName("POST /api/orders/update - 訂單不存在 (code=3001)")
    void updateOrder_NotFound() throws Exception {
        UpdateOrderRequest request = UpdateOrderRequest.builder()
                .items(List.of())
                .build();

        mockMvc.perform(post("/api/orders/update")
                        .param("id", "99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("訂單不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/orders/update - 非草稿訂單 (code=2003)")
    void updateOrder_NotDraft() throws Exception {
        UpdateOrderRequest request = UpdateOrderRequest.builder()
                .items(List.of())
                .build();

        mockMvc.perform(post("/api/orders/update")
                        .param("id", completedOrder.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("只有草稿狀態的訂單可以操作"));
    }

    // ========== 刪除訂單 POST /api/orders/delete ==========

    @Test
    @DisplayName("POST /api/orders/delete - 刪除訂單成功")
    void deleteOrder_Success() throws Exception {
        mockMvc.perform(post("/api/orders/delete")
                        .param("id", testOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true));

        // 驗證訂單已刪除
        assertThat(orderRepository.findById(testOrder.getId())).isEmpty();
    }

    @Test
    @DisplayName("POST /api/orders/delete - 訂單不存在 (code=3001)")
    void deleteOrder_NotFound() throws Exception {
        mockMvc.perform(post("/api/orders/delete")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("訂單不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/orders/delete - 非草稿訂單 (code=2003)")
    void deleteOrder_NotDraft() throws Exception {
        mockMvc.perform(post("/api/orders/delete")
                        .param("id", completedOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("只有草稿狀態的訂單可以操作"));
    }

    // ========== 查詢訂單詳情 GET /api/orders/detail ==========

    @Test
    @DisplayName("GET /api/orders/detail - 查詢訂單成功")
    void getOrderDetail_Success() throws Exception {
        mockMvc.perform(get("/api/orders/detail")
                        .param("id", testOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testOrder.getId()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.orderType").value("DINE_IN"));
    }

    @Test
    @DisplayName("GET /api/orders/detail - 訂單不存在 (code=3001)")
    void getOrderDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/orders/detail")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("訂單不存在: 99999"));
    }

    // ========== 分頁查詢訂單列表 GET /api/orders/list ==========

    @Test
    @DisplayName("GET /api/orders/list - 分頁查詢成功")
    void listOrders_Success() throws Exception {
        mockMvc.perform(get("/api/orders/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(4)); // testOrder + completedOrder + paidOrder + cancelledOrder
    }

    @Test
    @DisplayName("GET /api/orders/list - 篩選狀態")
    void listOrders_FilterByStatus() throws Exception {
        mockMvc.perform(get("/api/orders/list")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("DRAFT"));
    }

    @Test
    @DisplayName("GET /api/orders/list - 篩選類型")
    void listOrders_FilterByOrderType() throws Exception {
        mockMvc.perform(get("/api/orders/list")
                        .param("orderType", "TAKEOUT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].orderType").value("TAKEOUT"));
    }

    // ========== 完成訂單 POST /api/orders/complete ==========

    @Test
    @DisplayName("POST /api/orders/complete - 完成訂單成功")
    void completeOrder_Success() throws Exception {
        mockMvc.perform(post("/api/orders/complete")
                        .param("id", paidOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 驗證資料庫狀態
        Order updated = orderRepository.findById(paidOrder.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("POST /api/orders/complete - 訂單不存在 (code=3001)")
    void completeOrder_NotFound() throws Exception {
        mockMvc.perform(post("/api/orders/complete")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("訂單不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/orders/complete - 非已付款狀態 (code=2003)")
    void completeOrder_NotPaid() throws Exception {
        mockMvc.perform(post("/api/orders/complete")
                        .param("id", completedOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("只有已付款狀態的訂單可以完成"));
    }

    // ========== 完整流程測試 ==========
    // fullFlow_CreateUpdateComplete 已移至 OrderFullFlowIntegrationTest（需要異步事件處理）

    @Test
    @DisplayName("完整流程: 多選項正確計算")
    void fullFlow_WithMultipleOptions() throws Exception {
        List<OrderItemOptionDTO> options = List.of(
                OrderItemOptionDTO.builder()
                        .groupName("加料")
                        .valueName("加起司")
                        .priceAdjustment(new BigDecimal("10.00"))
                        .build(),
                OrderItemOptionDTO.builder()
                        .groupName("加料")
                        .valueName("加蛋")
                        .priceAdjustment(new BigDecimal("15.00"))
                        .build()
        );

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(testProduct.getId())
                                .quantity(2)
                                .options(options)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.items[0].optionsAmount").value(25.00))  // 10 + 15
                .andExpect(jsonPath("$.data.items[0].subtotal").value(168.00))      // (59 + 25) * 2
                .andExpect(jsonPath("$.data.totalAmount").value(168.00));
    }

    // ========== 取消訂單 POST /api/orders/cancel ==========

    @Test
    @DisplayName("POST /api/orders/cancel - 取消草稿訂單成功")
    void cancelOrder_Draft_Success() throws Exception {
        CancelOrderRequest request = CancelOrderRequest.builder()
                .reason("客戶改變心意")
                .build();

        mockMvc.perform(post("/api/orders/cancel")
                        .param("id", testOrder.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(testOrder.getId()))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.refundAmount").isEmpty())
                .andExpect(jsonPath("$.data.refundTransactionId").isEmpty());

        // 驗證資料庫狀態
        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("CANCELLED");
        assertThat(updated.getIsCancelled()).isTrue();
        assertThat(updated.getCancelledAt()).isNotNull();
        assertThat(updated.getCancelReason()).isEqualTo("客戶改變心意");
    }

    @Test
    @DisplayName("POST /api/orders/cancel - 取消已付款訂單成功並退款")
    void cancelOrder_Paid_Success_WithRefund() throws Exception {
        CancelOrderRequest request = CancelOrderRequest.builder()
                .reason("餐點有問題")
                .build();

        mockMvc.perform(post("/api/orders/cancel")
                        .param("id", paidOrder.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(paidOrder.getId()))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.refundAmount").value(59.00))
                .andExpect(jsonPath("$.data.refundTransactionId").isNumber());

        // 驗證訂單狀態
        Order updated = orderRepository.findById(paidOrder.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("CANCELLED");
        assertThat(updated.getIsCancelled()).isTrue();
        assertThat(updated.getCancelReason()).isEqualTo("餐點有問題");

        // 驗證原付款記錄被標記取消
        PaymentTransaction originalPayment = paymentTransactionRepository.findById(paidOrderPayment.getId()).orElseThrow();
        assertThat(originalPayment.getIsCancelled()).isTrue();

        // 驗證建立了退款記錄
        List<PaymentTransaction> transactions = paymentTransactionRepository.findByOrderIdOrderByIdDesc(paidOrder.getId());
        assertThat(transactions).hasSize(2);
        PaymentTransaction refund = transactions.get(0); // 最新的是退款
        assertThat(refund.getTransactionType()).isEqualTo("REFUND");
        assertThat(refund.getAmount()).isEqualByComparingTo(new BigDecimal("-59.00"));
        assertThat(refund.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("POST /api/orders/cancel - 取消已完成訂單失敗 (code=2003)")
    void cancelOrder_Completed_Fail() throws Exception {
        mockMvc.perform(post("/api/orders/cancel")
                        .param("id", completedOrder.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("已完成訂單無法取消"));
    }

    @Test
    @DisplayName("POST /api/orders/cancel - 訂單不存在 (code=3001)")
    void cancelOrder_NotFound() throws Exception {
        mockMvc.perform(post("/api/orders/cancel")
                        .param("id", "99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("訂單不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/orders/cancel - 重複取消失敗 (code=2003)")
    void cancelOrder_AlreadyCancelled_Fail() throws Exception {
        mockMvc.perform(post("/api/orders/cancel")
                        .param("id", cancelledOrder.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("訂單已取消"));
    }

    @Test
    @DisplayName("POST /api/orders/cancel - 不帶 reason 也能取消")
    void cancelOrder_WithoutReason_Success() throws Exception {
        mockMvc.perform(post("/api/orders/cancel")
                        .param("id", testOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // 驗證資料庫狀態
        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertThat(updated.getIsCancelled()).isTrue();
        assertThat(updated.getCancelReason()).isNull();
    }
}
