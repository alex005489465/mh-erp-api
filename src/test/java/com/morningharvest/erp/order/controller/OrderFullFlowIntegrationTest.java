package com.morningharvest.erp.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.order.dto.*;
import com.morningharvest.erp.order.repository.OrderItemRepository;
import com.morningharvest.erp.order.repository.OrderRepository;
import com.morningharvest.erp.payment.dto.CheckoutRequest;
import com.morningharvest.erp.payment.repository.PaymentTransactionRepository;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.entity.ProductCategory;
import com.morningharvest.erp.product.repository.ProductCategoryRepository;
import com.morningharvest.erp.product.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 訂單完整流程整合測試
 *
 * 不使用 @Transactional，以確保 @Async 事件監聯器能正常執行
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("訂單完整流程整合測試 (POS)")
class OrderFullFlowIntegrationTest {

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
    private PaymentTransactionRepository paymentTransactionRepository;

    private Product testProduct;
    private ProductCategory testCategory;

    @BeforeEach
    void setUp() {
        // 清理測試資料
        paymentTransactionRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();

        // 建立測試分類
        testCategory = ProductCategory.builder()
                .name("漢堡類")
                .isActive(true)
                .sortOrder(1)
                .build();
        testCategory = productCategoryRepository.save(testCategory);

        // 建立測試商品
        testProduct = Product.builder()
                .name("招牌漢堡")
                .price(new BigDecimal("59.00"))
                .categoryId(testCategory.getId())
                .isActive(true)
                .sortOrder(1)
                .build();
        testProduct = productRepository.save(testProduct);
    }

    @AfterEach
    void tearDown() {
        // 清理測試資料
        paymentTransactionRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteById(testProduct.getId());
        productCategoryRepository.deleteById(testCategory.getId());
    }

    @Test
    @DisplayName("完整流程: 建立訂單 → 查詢 → 更新 → 送出 → 付款 → 完成")
    void fullFlow_CreateUpdateComplete() throws Exception {
        // 1. 建立訂單（含項目）- 使用 POS 端點
        CreateOrderRequest createRequest = CreateOrderRequest.builder()
                .orderType("DINE_IN")
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(testProduct.getId())
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

        // 2. 查詢訂單 - 使用 ERP 端點
        mockMvc.perform(get("/api/orders/detail")
                        .param("id", orderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].productName").value("招牌漢堡"));

        // 3. 更新訂單（加一份、改外帶）- 使用 POS 端點
        UpdateOrderRequest updateRequest = UpdateOrderRequest.builder()
                .orderType("TAKEOUT")
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(testProduct.getId())
                                .quantity(2)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/pos/orders/update")
                        .param("id", orderId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.orderType").value("TAKEOUT"))
                .andExpect(jsonPath("$.data.totalAmount").value(118.00)); // 59 * 2

        // 4. 送出訂單 (DRAFT → PENDING_PAYMENT) - 使用 POS 端點
        mockMvc.perform(post("/api/pos/orders/submit")
                        .param("id", orderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));

        // 等待異步事件處理完成（付款條目由 @Async 事件監聯器建立）
        Thread.sleep(500);

        // 5. 付款 (PENDING_PAYMENT → PAID) - 使用 POS 端點
        CheckoutRequest checkoutRequest = CheckoutRequest.builder()
                .orderId(orderId)
                .paymentMethod("CASH")
                .amountReceived(new BigDecimal("118.00"))
                .changeAmount(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post("/api/pos/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        // 等待異步事件處理完成（訂單狀態由 @Async 事件監聯器更新為 PAID）
        Thread.sleep(500);

        // 6. 完成訂單 (PAID → COMPLETED) - 使用 POS 端點
        mockMvc.perform(post("/api/pos/orders/complete")
                        .param("id", orderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 7. 確認無法再更新 - 使用 POS 端點
        mockMvc.perform(post("/api/pos/orders/update")
                        .param("id", orderId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003));
    }
}
