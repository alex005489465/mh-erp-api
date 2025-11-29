package com.morningharvest.erp.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.order.dto.AddItemRequest;
import com.morningharvest.erp.order.dto.CreateOrderRequest;
import com.morningharvest.erp.order.dto.OrderItemOptionDTO;
import com.morningharvest.erp.order.dto.UpdateItemRequest;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.entity.OrderItem;
import com.morningharvest.erp.order.repository.OrderItemRepository;
import com.morningharvest.erp.order.repository.OrderRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    private Order testOrder;
    private Order completedOrder;
    private Product testProduct;
    private Product inactiveProduct;
    private OrderItem testOrderItem;
    private ProductCategory testCategory;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();

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

        // 建立測試訂單項目
        testOrderItem = OrderItem.builder()
                .orderId(testOrder.getId())
                .productId(testProduct.getId())
                .productName(testProduct.getName())
                .unitPrice(testProduct.getPrice())
                .quantity(1)
                .subtotal(testProduct.getPrice())
                .optionsAmount(BigDecimal.ZERO)
                .note("少辣")
                .build();
        testOrderItem = orderItemRepository.save(testOrderItem);

        // 更新訂單總金額
        testOrder.setTotalAmount(testOrderItem.getSubtotal());
        testOrder = orderRepository.save(testOrder);
    }

    // ========== 建立訂單 POST /api/orders/create ==========

    @Test
    @DisplayName("POST /api/orders/create - 建立訂單成功")
    void createOrder_Success() throws Exception {
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
    @DisplayName("POST /api/orders/create - 指定類型成功")
    void createOrder_WithOrderType_Success() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .orderType("TAKEOUT")
                .note("外帶訂單")
                .build();

        mockMvc.perform(post("/api/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderType").value("TAKEOUT"))
                .andExpect(jsonPath("$.data.note").value("外帶訂單"));
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
    @DisplayName("GET /api/orders/detail - 含項目查詢成功")
    void getOrderDetail_WithItems_Success() throws Exception {
        mockMvc.perform(get("/api/orders/detail")
                        .param("id", testOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].productName").value("招牌漢堡"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(1));
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
                .andExpect(jsonPath("$.data.totalElements").value(2)); // testOrder + completedOrder
    }

    @Test
    @DisplayName("GET /api/orders/list - 自訂分頁成功")
    void listOrders_WithPagination() throws Exception {
        mockMvc.perform(get("/api/orders/list")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10));
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

    @Test
    @DisplayName("GET /api/orders/list - 組合篩選")
    void listOrders_FilterByBoth() throws Exception {
        mockMvc.perform(get("/api/orders/list")
                        .param("status", "DRAFT")
                        .param("orderType", "DINE_IN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.data.content[0].orderType").value("DINE_IN"));
    }

    // ========== 完成訂單 POST /api/orders/complete ==========

    @Test
    @DisplayName("POST /api/orders/complete - 完成訂單成功")
    void completeOrder_Success() throws Exception {
        mockMvc.perform(post("/api/orders/complete")
                        .param("id", testOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 驗證資料庫狀態
        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
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
    @DisplayName("POST /api/orders/complete - 非草稿狀態 (code=2003)")
    void completeOrder_NotDraft() throws Exception {
        mockMvc.perform(post("/api/orders/complete")
                        .param("id", completedOrder.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("只有草稿狀態的訂單可以完成"));
    }

    // ========== 加入商品 POST /api/orders/items/add ==========

    @Test
    @DisplayName("POST /api/orders/items/add - 加入商品成功")
    void addItem_Success() throws Exception {
        AddItemRequest request = AddItemRequest.builder()
                .orderId(testOrder.getId())
                .productId(testProduct.getId())
                .quantity(2)
                .note("不要酸黃瓜")
                .build();

        mockMvc.perform(post("/api/orders/items/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productName").value("招牌漢堡"))
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andExpect(jsonPath("$.data.subtotal").value(118.00)); // 59 * 2
    }

    @Test
    @DisplayName("POST /api/orders/items/add - 含選項成功")
    void addItem_WithOptions_Success() throws Exception {
        List<OrderItemOptionDTO> options = List.of(
                OrderItemOptionDTO.builder()
                        .groupName("加料")
                        .valueName("加起司")
                        .priceAdjustment(new BigDecimal("10.00"))
                        .build()
        );

        AddItemRequest request = AddItemRequest.builder()
                .orderId(testOrder.getId())
                .productId(testProduct.getId())
                .quantity(1)
                .options(options)
                .build();

        mockMvc.perform(post("/api/orders/items/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.optionsAmount").value(10.00))
                .andExpect(jsonPath("$.data.subtotal").value(69.00)); // (59 + 10) * 1
    }

    @Test
    @DisplayName("POST /api/orders/items/add - 參數驗證失敗 (code=2001)")
    void addItem_ValidationError() throws Exception {
        // 缺少必填欄位 orderId 和 productId
        String invalidRequest = "{ \"quantity\": 1 }";

        mockMvc.perform(post("/api/orders/items/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.orderId").exists())
                .andExpect(jsonPath("$.data.productId").exists());
    }

    @Test
    @DisplayName("POST /api/orders/items/add - 訂單不存在 (code=3001)")
    void addItem_OrderNotFound() throws Exception {
        AddItemRequest request = AddItemRequest.builder()
                .orderId(99999L)
                .productId(testProduct.getId())
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/orders/items/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("訂單不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/orders/items/add - 商品不存在 (code=3001)")
    void addItem_ProductNotFound() throws Exception {
        AddItemRequest request = AddItemRequest.builder()
                .orderId(testOrder.getId())
                .productId(99999L)
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/orders/items/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("商品不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/orders/items/add - 商品已下架 (code=2002)")
    void addItem_ProductInactive() throws Exception {
        AddItemRequest request = AddItemRequest.builder()
                .orderId(testOrder.getId())
                .productId(inactiveProduct.getId())
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/orders/items/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("商品已下架: 停售商品"));
    }

    @Test
    @DisplayName("POST /api/orders/items/add - 非草稿訂單 (code=2003)")
    void addItem_OrderNotDraft() throws Exception {
        AddItemRequest request = AddItemRequest.builder()
                .orderId(completedOrder.getId())
                .productId(testProduct.getId())
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/orders/items/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("只有草稿狀態的訂單可以新增項目"));
    }

    // ========== 更新項目 POST /api/orders/items/update ==========

    @Test
    @DisplayName("POST /api/orders/items/update - 更新項目成功")
    void updateItem_Success() throws Exception {
        UpdateItemRequest request = UpdateItemRequest.builder()
                .itemId(testOrderItem.getId())
                .quantity(3)
                .note("更新備註")
                .build();

        mockMvc.perform(post("/api/orders/items/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.quantity").value(3))
                .andExpect(jsonPath("$.data.note").value("更新備註"));
    }

    @Test
    @DisplayName("POST /api/orders/items/update - 參數驗證失敗 (code=2001)")
    void updateItem_ValidationError() throws Exception {
        // 缺少必填欄位 itemId
        String invalidRequest = "{ \"quantity\": 2 }";

        mockMvc.perform(post("/api/orders/items/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.itemId").exists());
    }

    @Test
    @DisplayName("POST /api/orders/items/update - 項目不存在 (code=3001)")
    void updateItem_NotFound() throws Exception {
        UpdateItemRequest request = UpdateItemRequest.builder()
                .itemId(99999L)
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/orders/items/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("訂單項目不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/orders/items/update - 非草稿訂單 (code=2003)")
    void updateItem_OrderNotDraft() throws Exception {
        // 建立已完成訂單的項目
        OrderItem completedOrderItem = OrderItem.builder()
                .orderId(completedOrder.getId())
                .productId(testProduct.getId())
                .productName(testProduct.getName())
                .unitPrice(testProduct.getPrice())
                .quantity(1)
                .subtotal(testProduct.getPrice())
                .optionsAmount(BigDecimal.ZERO)
                .build();
        completedOrderItem = orderItemRepository.save(completedOrderItem);

        UpdateItemRequest request = UpdateItemRequest.builder()
                .itemId(completedOrderItem.getId())
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/orders/items/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("只有草稿狀態的訂單可以修改項目"));
    }

    @Test
    @DisplayName("POST /api/orders/items/update - 驗證小計重新計算")
    void updateItem_VerifySubtotalRecalculated() throws Exception {
        UpdateItemRequest request = UpdateItemRequest.builder()
                .itemId(testOrderItem.getId())
                .quantity(5)
                .build();

        mockMvc.perform(post("/api/orders/items/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.quantity").value(5))
                .andExpect(jsonPath("$.data.subtotal").value(295.00)); // 59 * 5

        // 驗證訂單總金額也更新
        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertThat(updated.getTotalAmount()).isEqualByComparingTo(new BigDecimal("295.00"));
    }

    // ========== 移除項目 POST /api/orders/items/remove ==========

    @Test
    @DisplayName("POST /api/orders/items/remove - 移除項目成功")
    void removeItem_Success() throws Exception {
        mockMvc.perform(post("/api/orders/items/remove")
                        .param("id", testOrderItem.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true));

        // 驗證項目已刪除
        assertThat(orderItemRepository.findById(testOrderItem.getId())).isEmpty();
    }

    @Test
    @DisplayName("POST /api/orders/items/remove - 項目不存在 (code=3001)")
    void removeItem_NotFound() throws Exception {
        mockMvc.perform(post("/api/orders/items/remove")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("訂單項目不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/orders/items/remove - 非草稿訂單 (code=2003)")
    void removeItem_OrderNotDraft() throws Exception {
        // 建立已完成訂單的項目
        OrderItem completedOrderItem = OrderItem.builder()
                .orderId(completedOrder.getId())
                .productId(testProduct.getId())
                .productName(testProduct.getName())
                .unitPrice(testProduct.getPrice())
                .quantity(1)
                .subtotal(testProduct.getPrice())
                .optionsAmount(BigDecimal.ZERO)
                .build();
        completedOrderItem = orderItemRepository.save(completedOrderItem);

        mockMvc.perform(post("/api/orders/items/remove")
                        .param("id", completedOrderItem.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("只有草稿狀態的訂單可以刪除項目"));
    }

    @Test
    @DisplayName("POST /api/orders/items/remove - 驗證總金額重新計算")
    void removeItem_VerifyTotalRecalculated() throws Exception {
        // 新增第二個項目
        OrderItem secondItem = OrderItem.builder()
                .orderId(testOrder.getId())
                .productId(testProduct.getId())
                .productName(testProduct.getName())
                .unitPrice(testProduct.getPrice())
                .quantity(2)
                .subtotal(new BigDecimal("118.00"))
                .optionsAmount(BigDecimal.ZERO)
                .build();
        secondItem = orderItemRepository.save(secondItem);

        // 更新訂單總金額: 59 + 118 = 177
        testOrder.setTotalAmount(new BigDecimal("177.00"));
        orderRepository.save(testOrder);

        // 移除第一個項目
        mockMvc.perform(post("/api/orders/items/remove")
                        .param("id", testOrderItem.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        // 驗證總金額已更新: 只剩下 118
        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertThat(updated.getTotalAmount()).isEqualByComparingTo(new BigDecimal("118.00"));
    }
}
