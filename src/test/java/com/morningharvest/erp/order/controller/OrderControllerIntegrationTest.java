package com.morningharvest.erp.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.repository.OrderItemRepository;
import com.morningharvest.erp.order.repository.OrderRepository;
import com.morningharvest.erp.payment.repository.PaymentTransactionRepository;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.entity.ProductCategory;
import com.morningharvest.erp.product.repository.ProductCategoryRepository;
import com.morningharvest.erp.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OrderController 整合測試
 * 測試 ERP 訂單查詢功能 (detail, list)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("OrderController 整合測試 (ERP 查詢)")
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
    private PaymentTransactionRepository paymentTransactionRepository;

    private Order testOrder;
    private Order completedOrder;
    private Order paidOrder;
    private Order cancelledOrder;
    private Product testProduct;
    private ProductCategory testCategory;

    @BeforeEach
    void setUp() {
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

        // 建立已付款訂單
        paidOrder = Order.builder()
                .status("PAID")
                .orderType("DINE_IN")
                .totalAmount(new BigDecimal("59.00"))
                .build();
        paidOrder = orderRepository.save(paidOrder);

        // 建立已取消訂單
        cancelledOrder = Order.builder()
                .status("CANCELLED")
                .orderType("DINE_IN")
                .totalAmount(new BigDecimal("59.00"))
                .isCancelled(true)
                .cancelReason("客戶取消")
                .build();
        cancelledOrder = orderRepository.save(cancelledOrder);
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
    @DisplayName("GET /api/orders/detail - 查詢已完成訂單")
    void getOrderDetail_Completed_Success() throws Exception {
        mockMvc.perform(get("/api/orders/detail")
                        .param("id", completedOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.orderType").value("TAKEOUT"));
    }

    @Test
    @DisplayName("GET /api/orders/detail - 查詢已取消訂單")
    void getOrderDetail_Cancelled_Success() throws Exception {
        mockMvc.perform(get("/api/orders/detail")
                        .param("id", cancelledOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
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
    @DisplayName("GET /api/orders/list - 篩選狀態 DRAFT")
    void listOrders_FilterByStatus_Draft() throws Exception {
        mockMvc.perform(get("/api/orders/list")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("DRAFT"));
    }

    @Test
    @DisplayName("GET /api/orders/list - 篩選狀態 COMPLETED")
    void listOrders_FilterByStatus_Completed() throws Exception {
        mockMvc.perform(get("/api/orders/list")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/orders/list - 篩選狀態 CANCELLED")
    void listOrders_FilterByStatus_Cancelled() throws Exception {
        mockMvc.perform(get("/api/orders/list")
                        .param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("CANCELLED"));
    }

    @Test
    @DisplayName("GET /api/orders/list - 篩選類型 TAKEOUT")
    void listOrders_FilterByOrderType() throws Exception {
        mockMvc.perform(get("/api/orders/list")
                        .param("orderType", "TAKEOUT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].orderType").value("TAKEOUT"));
    }

    @Test
    @DisplayName("GET /api/orders/list - 組合篩選 (狀態 + 類型)")
    void listOrders_FilterByStatusAndType() throws Exception {
        mockMvc.perform(get("/api/orders/list")
                        .param("status", "DRAFT")
                        .param("orderType", "DINE_IN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.data.content[0].orderType").value("DINE_IN"));
    }

    @Test
    @DisplayName("GET /api/orders/list - 分頁參數")
    void listOrders_WithPagination() throws Exception {
        mockMvc.perform(get("/api/orders/list")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    @Test
    @DisplayName("GET /api/orders/list - 排序參數")
    void listOrders_WithSorting() throws Exception {
        mockMvc.perform(get("/api/orders/list")
                        .param("sortBy", "totalAmount")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
