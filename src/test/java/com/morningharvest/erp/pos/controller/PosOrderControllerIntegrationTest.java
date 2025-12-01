package com.morningharvest.erp.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.combo.entity.Combo;
import com.morningharvest.erp.combo.entity.ComboItem;
import com.morningharvest.erp.combo.repository.ComboItemRepository;
import com.morningharvest.erp.combo.repository.ComboRepository;
import com.morningharvest.erp.order.dto.CancelOrderRequest;
import com.morningharvest.erp.order.dto.CreateOrderRequest;
import com.morningharvest.erp.order.dto.OrderItemOptionDTO;
import com.morningharvest.erp.order.dto.OrderItemRequest;
import com.morningharvest.erp.order.dto.UpdateOrderRequest;
import com.morningharvest.erp.order.entity.Order;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("PosOrderController 整合測試")
class PosOrderControllerIntegrationTest {

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
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private ProductOptionGroupRepository optionGroupRepository;

    @Autowired
    private ProductOptionValueRepository optionValueRepository;

    @Autowired
    private ComboRepository comboRepository;

    @Autowired
    private ComboItemRepository comboItemRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    private ProductCategory category;
    private Product product1;
    private Product product2;
    private Combo combo;
    private ProductOptionGroup optionGroup;
    private ProductOptionValue optionValue;
    private Order draftOrder;

    @BeforeEach
    void setUp() {
        // 清理資料
        paymentTransactionRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        comboItemRepository.deleteAll();
        comboRepository.deleteAll();
        optionValueRepository.deleteAll();
        optionGroupRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // 建立分類
        category = categoryRepository.save(ProductCategory.builder()
                .name("早餐類")
                .isActive(true)
                .sortOrder(1)
                .build());

        // 建立商品
        product1 = productRepository.save(Product.builder()
                .name("招牌漢堡")
                .price(new BigDecimal("59.00"))
                .categoryId(category.getId())
                .categoryName(category.getName())
                .isActive(true)
                .sortOrder(1)
                .build());

        product2 = productRepository.save(Product.builder()
                .name("紅茶")
                .price(new BigDecimal("25.00"))
                .categoryId(category.getId())
                .categoryName(category.getName())
                .isActive(true)
                .sortOrder(2)
                .build());

        // 建立選項
        optionGroup = optionGroupRepository.save(ProductOptionGroup.builder()
                .productId(product1.getId())
                .name("加料")
                .minSelections(0)
                .maxSelections(3)
                .isActive(true)
                .sortOrder(1)
                .build());

        optionValue = optionValueRepository.save(ProductOptionValue.builder()
                .groupId(optionGroup.getId())
                .name("加起司")
                .priceAdjustment(new BigDecimal("10.00"))
                .isActive(true)
                .sortOrder(1)
                .build());

        // 建立套餐
        combo = comboRepository.save(Combo.builder()
                .name("超值早餐A")
                .price(new BigDecimal("79.00"))
                .categoryId(category.getId())
                .categoryName(category.getName())
                .isActive(true)
                .sortOrder(1)
                .build());

        comboItemRepository.save(ComboItem.builder()
                .comboId(combo.getId())
                .productId(product1.getId())
                .productName(product1.getName())
                .quantity(1)
                .sortOrder(1)
                .build());

        comboItemRepository.save(ComboItem.builder()
                .comboId(combo.getId())
                .productId(product2.getId())
                .productName(product2.getName())
                .quantity(1)
                .sortOrder(2)
                .build());

        // 建立草稿訂單
        draftOrder = orderRepository.save(Order.builder()
                .status("DRAFT")
                .orderType("DINE_IN")
                .totalAmount(new BigDecimal("100.00"))
                .build());
    }

    private Order paidOrder;

    // ========== POST /api/pos/orders/create ==========

    @Test
    @DisplayName("POST /api/pos/orders/create - 建立空訂單成功")
    void createOrder_Empty_Success() throws Exception {
        mockMvc.perform(post("/api/pos/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.orderType").value("DINE_IN"));
    }

    @Test
    @DisplayName("POST /api/pos/orders/create - 建立單點商品訂單成功")
    void createOrder_SingleProduct_Success() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .orderType("TAKEOUT")
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(product1.getId())
                                .quantity(2)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/pos/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.orderType").value("TAKEOUT"))
                .andExpect(jsonPath("$.data.items[0].productName").value("招牌漢堡"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.totalAmount").value(118.00)); // 59 * 2
    }

    @Test
    @DisplayName("POST /api/pos/orders/create - 建立含選項訂單成功")
    void createOrder_WithOptions_Success() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(product1.getId())
                                .quantity(1)
                                .options(List.of(
                                        OrderItemOptionDTO.builder()
                                                .groupName("加料")
                                                .valueName("加起司")
                                                .priceAdjustment(new BigDecimal("10.00"))
                                                .build()
                                ))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/pos/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.items[0].optionsAmount").value(10.00))
                .andExpect(jsonPath("$.data.totalAmount").value(69.00)); // 59 + 10
    }

    // ========== POST /api/pos/orders/update ==========

    @Test
    @DisplayName("POST /api/pos/orders/update - 更新訂單成功")
    void updateOrder_Success() throws Exception {
        UpdateOrderRequest request = UpdateOrderRequest.builder()
                .orderType("DELIVERY")
                .note("外送訂單")
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(product2.getId())
                                .quantity(3)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/pos/orders/update")
                        .param("id", draftOrder.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.orderType").value("DELIVERY"))
                .andExpect(jsonPath("$.data.note").value("外送訂單"))
                .andExpect(jsonPath("$.data.totalAmount").value(75.00)); // 25 * 3
    }

    // ========== POST /api/pos/orders/submit ==========

    @Test
    @DisplayName("POST /api/pos/orders/submit - 送出訂單成功")
    void submitOrder_Success() throws Exception {
        mockMvc.perform(post("/api/pos/orders/submit")
                        .param("id", draftOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));
    }

    @Test
    @DisplayName("POST /api/pos/orders/submit - 非草稿狀態失敗")
    void submitOrder_NotDraft_Fails() throws Exception {
        // 先送出訂單
        mockMvc.perform(post("/api/pos/orders/submit")
                        .param("id", draftOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        // 再次送出應該失敗
        mockMvc.perform(post("/api/pos/orders/submit")
                        .param("id", draftOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003));
    }

    // ========== POST /api/pos/orders/complete ==========

    @Test
    @DisplayName("POST /api/pos/orders/complete - 從 PAID 狀態完成訂單成功")
    void completeOrder_FromPaid_Success() throws Exception {
        // 建立已付款訂單
        paidOrder = orderRepository.save(Order.builder()
                .status("PAID")
                .orderType("DINE_IN")
                .totalAmount(new BigDecimal("100.00"))
                .build());

        mockMvc.perform(post("/api/pos/orders/complete")
                        .param("id", paidOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/pos/orders/complete - 非 PAID 狀態失敗")
    void completeOrder_NotPaid_Fails() throws Exception {
        // DRAFT 狀態的訂單不能直接完成
        mockMvc.perform(post("/api/pos/orders/complete")
                        .param("id", draftOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003));
    }

    // ========== GET /api/pos/orders/detail ==========

    @Test
    @DisplayName("GET /api/pos/orders/detail - 查詢訂單詳情成功")
    void getOrderDetail_Success() throws Exception {
        mockMvc.perform(get("/api/pos/orders/detail")
                        .param("id", draftOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.id").value(draftOrder.getId()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    // ========== GET /api/pos/orders/list ==========

    @Test
    @DisplayName("GET /api/pos/orders/list - 預設查詢草稿訂單")
    void listOrders_DefaultDraft() throws Exception {
        mockMvc.perform(get("/api/pos/orders/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].status").value("DRAFT"));
    }

    // ========== 完整流程測試 ==========

    @Test
    @DisplayName("完整 POS 流程: 查詢菜單 → 建立訂單 → 送出訂單")
    void fullPosFlow_Submit() throws Exception {
        // 1. 查詢菜單
        mockMvc.perform(get("/api/pos/menu/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        // 2. 建立訂單（使用 orderPayload 格式）
        CreateOrderRequest createRequest = CreateOrderRequest.builder()
                .orderType("DINE_IN")
                .items(List.of(
                        // 單點商品
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(product1.getId())
                                .quantity(1)
                                .build()
                ))
                .build();

        String createResponse = mockMvc.perform(post("/api/pos/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalAmount").value(59.00))
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // 3. 送出訂單（DRAFT → PENDING_PAYMENT）
        mockMvc.perform(post("/api/pos/orders/submit")
                        .param("id", orderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));

        // 4. 確認無法再更新（非草稿狀態）
        UpdateOrderRequest updateRequest = UpdateOrderRequest.builder()
                .items(List.of())
                .build();

        mockMvc.perform(post("/api/pos/orders/update")
                        .param("id", orderId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003)); // 非草稿狀態

        // 5. 確認無法直接完成（必須先付款）
        mockMvc.perform(post("/api/pos/orders/complete")
                        .param("id", orderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003)); // 非 PAID 狀態
    }

    // ========== POST /api/pos/orders/cancel ==========

    @Test
    @DisplayName("POST /api/pos/orders/cancel - 取消草稿訂單成功")
    void cancelOrder_Draft_Success() throws Exception {
        CancelOrderRequest request = CancelOrderRequest.builder()
                .reason("客戶改變心意")
                .build();

        mockMvc.perform(post("/api/pos/orders/cancel")
                        .param("id", draftOrder.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(draftOrder.getId()))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.refundAmount").isEmpty())
                .andExpect(jsonPath("$.data.refundTransactionId").isEmpty());
    }

    @Test
    @DisplayName("POST /api/pos/orders/cancel - 取消已付款訂單成功並退款")
    void cancelOrder_Paid_Success_WithRefund() throws Exception {
        // 建立已付款訂單
        Order paidOrderForCancel = orderRepository.save(Order.builder()
                .status("PAID")
                .orderType("DINE_IN")
                .totalAmount(new BigDecimal("59.00"))
                .build());

        // 建立付款記錄
        paymentTransactionRepository.save(PaymentTransaction.builder()
                .orderId(paidOrderForCancel.getId())
                .transactionType("PAYMENT")
                .paymentMethod("CASH")
                .status("COMPLETED")
                .amount(new BigDecimal("59.00"))
                .amountReceived(new BigDecimal("100.00"))
                .changeAmount(new BigDecimal("41.00"))
                .transactionTime(LocalDateTime.now())
                .build());

        CancelOrderRequest request = CancelOrderRequest.builder()
                .reason("餐點有問題")
                .build();

        mockMvc.perform(post("/api/pos/orders/cancel")
                        .param("id", paidOrderForCancel.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(paidOrderForCancel.getId()))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.refundAmount").value(59.00))
                .andExpect(jsonPath("$.data.refundTransactionId").isNumber());
    }

    @Test
    @DisplayName("POST /api/pos/orders/cancel - 取消已完成訂單失敗")
    void cancelOrder_Completed_Fail() throws Exception {
        // 建立已完成訂單
        Order completedOrder = orderRepository.save(Order.builder()
                .status("COMPLETED")
                .orderType("DINE_IN")
                .totalAmount(new BigDecimal("100.00"))
                .build());

        mockMvc.perform(post("/api/pos/orders/cancel")
                        .param("id", completedOrder.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("已完成訂單無法取消"));
    }

    @Test
    @DisplayName("POST /api/pos/orders/cancel - 訂單不存在")
    void cancelOrder_NotFound() throws Exception {
        mockMvc.perform(post("/api/pos/orders/cancel")
                        .param("id", "99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("訂單不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/pos/orders/cancel - 不帶 reason 也能取消")
    void cancelOrder_WithoutReason_Success() throws Exception {
        mockMvc.perform(post("/api/pos/orders/cancel")
                        .param("id", draftOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
