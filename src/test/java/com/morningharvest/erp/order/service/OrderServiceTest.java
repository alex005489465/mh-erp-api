package com.morningharvest.erp.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.order.dto.*;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.entity.OrderItem;
import com.morningharvest.erp.order.repository.OrderItemRepository;
import com.morningharvest.erp.order.repository.OrderRepository;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.entity.ProductOptionGroup;
import com.morningharvest.erp.product.entity.ProductOptionValue;
import com.morningharvest.erp.product.repository.ProductOptionGroupRepository;
import com.morningharvest.erp.product.repository.ProductOptionValueRepository;
import com.morningharvest.erp.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 單元測試")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductOptionGroupRepository optionGroupRepository;

    @Mock
    private ProductOptionValueRepository optionValueRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private Order completedOrder;
    private OrderItem testOrderItem;
    private Product testProduct;
    private Product inactiveProduct;
    private CreateOrderRequest createOrderRequest;
    private AddItemRequest addItemRequest;
    private UpdateItemRequest updateItemRequest;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(1L)
                .name("招牌漢堡")
                .price(new BigDecimal("59.00"))
                .isActive(true)
                .build();

        inactiveProduct = Product.builder()
                .id(2L)
                .name("停售商品")
                .price(new BigDecimal("49.00"))
                .isActive(false)
                .build();

        testOrder = Order.builder()
                .id(1L)
                .status("DRAFT")
                .orderType("DINE_IN")
                .totalAmount(BigDecimal.ZERO)
                .note("測試訂單")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        completedOrder = Order.builder()
                .id(2L)
                .status("COMPLETED")
                .orderType("TAKEOUT")
                .totalAmount(new BigDecimal("118.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testOrderItem = OrderItem.builder()
                .id(1L)
                .orderId(1L)
                .productId(1L)
                .productName("招牌漢堡")
                .unitPrice(new BigDecimal("59.00"))
                .quantity(1)
                .subtotal(new BigDecimal("59.00"))
                .optionsAmount(BigDecimal.ZERO)
                .note("少辣")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createOrderRequest = CreateOrderRequest.builder()
                .orderType("TAKEOUT")
                .note("外帶訂單")
                .build();

        addItemRequest = AddItemRequest.builder()
                .orderId(1L)
                .productId(1L)
                .quantity(2)
                .note("不要酸黃瓜")
                .build();

        updateItemRequest = UpdateItemRequest.builder()
                .itemId(1L)
                .quantity(3)
                .note("更新備註")
                .build();
    }

    // ========== createOrder 測試 ==========

    @Test
    @DisplayName("建立訂單 - 成功")
    void createOrder_Success() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder().build();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(1L);
            o.setCreatedAt(LocalDateTime.now());
            o.setUpdatedAt(LocalDateTime.now());
            return o;
        });

        // When
        OrderDTO result = orderService.createOrder(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("DRAFT");
        assertThat(result.getOrderType()).isEqualTo("DINE_IN");
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("建立訂單 - 指定訂單類型成功")
    void createOrder_WithOrderType_Success() {
        // Given
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(1L);
            o.setCreatedAt(LocalDateTime.now());
            o.setUpdatedAt(LocalDateTime.now());
            return o;
        });

        // When
        OrderDTO result = orderService.createOrder(createOrderRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderType()).isEqualTo("TAKEOUT");
        assertThat(result.getNote()).isEqualTo("外帶訂單");
    }

    @Test
    @DisplayName("建立訂單 - 含備註成功")
    void createOrder_WithNote_Success() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .note("特別備註")
                .build();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(1L);
            o.setCreatedAt(LocalDateTime.now());
            o.setUpdatedAt(LocalDateTime.now());
            return o;
        });

        // When
        OrderDTO result = orderService.createOrder(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getNote()).isEqualTo("特別備註");
    }

    // ========== getOrderById 測試 ==========

    @Test
    @DisplayName("查詢訂單 - 成功")
    void getOrderById_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(testOrderItem));

        // When
        OrderDetailDTO result = orderService.getOrderById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("DRAFT");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("招牌漢堡");
        verify(orderRepository).findById(1L);
        verify(orderItemRepository).findByOrderId(1L);
    }

    @Test
    @DisplayName("查詢訂單 - 無項目成功")
    void getOrderById_WithoutItems_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        // When
        OrderDetailDTO result = orderService.getOrderById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    @DisplayName("查詢訂單 - 訂單不存在拋出例外")
    void getOrderById_NotFound_ThrowsException() {
        // Given
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("訂單不存在");

        verify(orderRepository).findById(999L);
    }

    // ========== listOrders 測試 ==========

    @Test
    @DisplayName("分頁查詢訂單列表")
    void listOrders_WithPagination() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(orderPage);

        // When
        PageResponse<OrderDTO> result = orderService.listOrders(pageableRequest, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(orderRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("分頁查詢訂單 - 篩選狀態")
    void listOrders_FilterByStatus() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findByStatus(eq("DRAFT"), any(Pageable.class))).thenReturn(orderPage);

        // When
        PageResponse<OrderDTO> result = orderService.listOrders(pageableRequest, "DRAFT", null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findByStatus(eq("DRAFT"), any(Pageable.class));
    }

    @Test
    @DisplayName("分頁查詢訂單 - 篩選類型")
    void listOrders_FilterByOrderType() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findByOrderType(eq("DINE_IN"), any(Pageable.class))).thenReturn(orderPage);

        // When
        PageResponse<OrderDTO> result = orderService.listOrders(pageableRequest, null, "DINE_IN");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findByOrderType(eq("DINE_IN"), any(Pageable.class));
    }

    @Test
    @DisplayName("分頁查詢訂單 - 篩選狀態和類型")
    void listOrders_FilterByStatusAndOrderType() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findByStatusAndOrderType(eq("DRAFT"), eq("DINE_IN"), any(Pageable.class)))
                .thenReturn(orderPage);

        // When
        PageResponse<OrderDTO> result = orderService.listOrders(pageableRequest, "DRAFT", "DINE_IN");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findByStatusAndOrderType(eq("DRAFT"), eq("DINE_IN"), any(Pageable.class));
    }

    @Test
    @DisplayName("分頁查詢訂單 - 空結果")
    void listOrders_EmptyResult() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Order> orderPage = new PageImpl<>(Collections.emptyList());
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(orderPage);

        // When
        PageResponse<OrderDTO> result = orderService.listOrders(pageableRequest, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // ========== completeOrder 測試 ==========

    @Test
    @DisplayName("完成訂單 - 成功")
    void completeOrder_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OrderDTO result = orderService.completeOrder(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("完成訂單 - 訂單不存在拋出例外")
    void completeOrder_NotFound_ThrowsException() {
        // Given
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.completeOrder(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("訂單不存在");

        verify(orderRepository).findById(999L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("完成訂單 - 非草稿狀態拋出例外")
    void completeOrder_NotDraft_ThrowsException() {
        // Given
        when(orderRepository.findById(2L)).thenReturn(Optional.of(completedOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.completeOrder(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("只有草稿狀態的訂單可以完成");

        verify(orderRepository).findById(2L);
        verify(orderRepository, never()).save(any());
    }

    // ========== addItem 測試 ==========

    @Test
    @DisplayName("加入商品 - 成功")
    void addItem_Success() throws JsonProcessingException {
        // Given
        AddItemRequest request = AddItemRequest.builder()
                .orderId(1L)
                .productId(1L)
                .quantity(1)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            item.setId(1L);
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            return item;
        });
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        // When
        OrderItemDTO result = orderService.addItem(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductName()).isEqualTo("招牌漢堡");
        assertThat(result.getUnitPrice()).isEqualByComparingTo(new BigDecimal("59.00"));
        verify(orderRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("加入商品 - 含數量成功")
    void addItem_WithQuantity_Success() throws JsonProcessingException {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            item.setId(1L);
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            return item;
        });
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        // When
        OrderItemDTO result = orderService.addItem(addItemRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(2);
        // 小計: 59.00 * 2 = 118.00
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("118.00"));
    }

    @Test
    @DisplayName("加入商品 - 選項加價正確計算")
    void addItem_WithOptions_CalculatesCorrectAmount() throws JsonProcessingException {
        // Given
        ProductOptionGroup group = ProductOptionGroup.builder()
                .id(1L)
                .productId(1L)
                .name("加料")
                .isActive(true)
                .build();

        ProductOptionValue value = ProductOptionValue.builder()
                .id(1L)
                .groupId(1L)
                .name("加起司")
                .priceAdjustment(new BigDecimal("10.00"))
                .isActive(true)
                .build();

        List<OrderItemOptionDTO> options = List.of(
                OrderItemOptionDTO.builder()
                        .groupName("加料")
                        .valueName("加起司")
                        .priceAdjustment(new BigDecimal("10.00"))
                        .build()
        );

        AddItemRequest request = AddItemRequest.builder()
                .orderId(1L)
                .productId(1L)
                .quantity(1)
                .options(options)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(optionGroupRepository.findByProductIdAndIsActiveOrderBySortOrder(1L, true))
                .thenReturn(List.of(group));
        when(optionValueRepository.findByGroupIdInOrderByGroupIdAndSortOrder(List.of(1L)))
                .thenReturn(List.of(value));
        when(objectMapper.writeValueAsString(any())).thenReturn("[{\"groupName\":\"加料\"}]");
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            item.setId(1L);
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            return item;
        });
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        // When
        OrderItemDTO result = orderService.addItem(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOptionsAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        // 小計: (59.00 + 10.00) * 1 = 69.00
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("69.00"));
    }

    @Test
    @DisplayName("加入商品 - 選項和數量計算小計")
    void addItem_WithOptionsAndQuantity_CalculatesSubtotal() throws JsonProcessingException {
        // Given
        ProductOptionGroup group1 = ProductOptionGroup.builder()
                .id(1L)
                .productId(1L)
                .name("加料")
                .isActive(true)
                .build();

        ProductOptionGroup group2 = ProductOptionGroup.builder()
                .id(2L)
                .productId(1L)
                .name("辣度")
                .isActive(true)
                .build();

        ProductOptionValue value1 = ProductOptionValue.builder()
                .id(1L)
                .groupId(1L)
                .name("加起司")
                .priceAdjustment(new BigDecimal("10.00"))
                .isActive(true)
                .build();

        ProductOptionValue value2 = ProductOptionValue.builder()
                .id(2L)
                .groupId(2L)
                .name("小辣")
                .priceAdjustment(BigDecimal.ZERO)
                .isActive(true)
                .build();

        List<OrderItemOptionDTO> options = List.of(
                OrderItemOptionDTO.builder()
                        .groupName("加料")
                        .valueName("加起司")
                        .priceAdjustment(new BigDecimal("10.00"))
                        .build(),
                OrderItemOptionDTO.builder()
                        .groupName("辣度")
                        .valueName("小辣")
                        .priceAdjustment(BigDecimal.ZERO)
                        .build()
        );

        AddItemRequest request = AddItemRequest.builder()
                .orderId(1L)
                .productId(1L)
                .quantity(2)
                .options(options)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(optionGroupRepository.findByProductIdAndIsActiveOrderBySortOrder(1L, true))
                .thenReturn(List.of(group1, group2));
        when(optionValueRepository.findByGroupIdInOrderByGroupIdAndSortOrder(List.of(1L, 2L)))
                .thenReturn(List.of(value1, value2));
        when(objectMapper.writeValueAsString(any())).thenReturn("[{}]");
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            item.setId(1L);
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            return item;
        });
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        // When
        OrderItemDTO result = orderService.addItem(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOptionsAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        // 小計: (59.00 + 10.00) * 2 = 138.00
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("138.00"));
    }

    @Test
    @DisplayName("加入商品 - 訂單不存在拋出例外")
    void addItem_OrderNotFound_ThrowsException() {
        // Given
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.addItem(addItemRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("訂單不存在");

        verify(orderRepository).findById(1L);
        verify(productRepository, never()).findById(anyLong());
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("加入商品 - 商品不存在拋出例外")
    void addItem_ProductNotFound_ThrowsException() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.addItem(addItemRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("商品不存在");

        verify(orderRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("加入商品 - 商品已下架拋出例外")
    void addItem_ProductInactive_ThrowsException() {
        // Given
        AddItemRequest request = AddItemRequest.builder()
                .orderId(1L)
                .productId(2L)
                .quantity(1)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(productRepository.findById(2L)).thenReturn(Optional.of(inactiveProduct));

        // When & Then
        assertThatThrownBy(() -> orderService.addItem(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("商品已下架");

        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("加入商品 - 非草稿訂單拋出例外")
    void addItem_OrderNotDraft_ThrowsException() {
        // Given
        AddItemRequest request = AddItemRequest.builder()
                .orderId(2L)
                .productId(1L)
                .quantity(1)
                .build();

        when(orderRepository.findById(2L)).thenReturn(Optional.of(completedOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.addItem(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("只有草稿狀態的訂單可以新增項目");

        verify(productRepository, never()).findById(anyLong());
        verify(orderItemRepository, never()).save(any());
    }

    // ========== updateItem 測試 ==========

    @Test
    @DisplayName("更新項目 - 成功")
    void updateItem_Success() {
        // Given
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testOrderItem));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(testOrderItem));

        // When
        OrderItemDTO result = orderService.updateItem(updateItemRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(3);
        assertThat(result.getNote()).isEqualTo("更新備註");
        verify(orderItemRepository).findById(1L);
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("更新項目 - 更新數量重新計算小計")
    void updateItem_UpdateQuantity_RecalculatesSubtotal() {
        // Given
        UpdateItemRequest request = UpdateItemRequest.builder()
                .itemId(1L)
                .quantity(5)
                .build();

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testOrderItem));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(testOrderItem));

        // When
        OrderItemDTO result = orderService.updateItem(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(5);
        // 小計: 59.00 * 5 = 295.00
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("295.00"));
    }

    @Test
    @DisplayName("更新項目 - 更新選項重新計算小計")
    void updateItem_WithOptions_RecalculatesSubtotal() throws JsonProcessingException {
        // Given
        ProductOptionGroup group = ProductOptionGroup.builder()
                .id(1L)
                .productId(1L)
                .name("加料")
                .isActive(true)
                .build();

        ProductOptionValue value = ProductOptionValue.builder()
                .id(1L)
                .groupId(1L)
                .name("加蛋")
                .priceAdjustment(new BigDecimal("15.00"))
                .isActive(true)
                .build();

        List<OrderItemOptionDTO> options = List.of(
                OrderItemOptionDTO.builder()
                        .groupName("加料")
                        .valueName("加蛋")
                        .priceAdjustment(new BigDecimal("15.00"))
                        .build()
        );

        UpdateItemRequest request = UpdateItemRequest.builder()
                .itemId(1L)
                .options(options)
                .build();

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testOrderItem));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(optionGroupRepository.findByProductIdAndIsActiveOrderBySortOrder(1L, true))
                .thenReturn(List.of(group));
        when(optionValueRepository.findByGroupIdInOrderByGroupIdAndSortOrder(List.of(1L)))
                .thenReturn(List.of(value));
        when(objectMapper.writeValueAsString(any())).thenReturn("[{}]");
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(testOrderItem));

        // When
        OrderItemDTO result = orderService.updateItem(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOptionsAmount()).isEqualByComparingTo(new BigDecimal("15.00"));
        // 小計: (59.00 + 15.00) * 1 = 74.00
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("74.00"));
    }

    @Test
    @DisplayName("更新項目 - 項目不存在拋出例外")
    void updateItem_NotFound_ThrowsException() {
        // Given
        UpdateItemRequest request = UpdateItemRequest.builder()
                .itemId(999L)
                .quantity(2)
                .build();

        when(orderItemRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.updateItem(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("訂單項目不存在");

        verify(orderItemRepository).findById(999L);
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新項目 - 訂單不存在拋出例外")
    void updateItem_OrderNotFound_ThrowsException() {
        // Given
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testOrderItem));
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.updateItem(updateItemRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("訂單不存在");

        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新項目 - 非草稿訂單拋出例外")
    void updateItem_OrderNotDraft_ThrowsException() {
        // Given
        OrderItem itemInCompletedOrder = OrderItem.builder()
                .id(10L)
                .orderId(2L)
                .productId(1L)
                .productName("招牌漢堡")
                .unitPrice(new BigDecimal("59.00"))
                .quantity(1)
                .subtotal(new BigDecimal("59.00"))
                .optionsAmount(BigDecimal.ZERO)
                .build();

        UpdateItemRequest request = UpdateItemRequest.builder()
                .itemId(10L)
                .quantity(2)
                .build();

        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(itemInCompletedOrder));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(completedOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.updateItem(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("只有草稿狀態的訂單可以修改項目");

        verify(orderItemRepository, never()).save(any());
    }

    // ========== removeItem 測試 ==========

    @Test
    @DisplayName("移除項目 - 成功")
    void removeItem_Success() {
        // Given
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testOrderItem));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        doNothing().when(orderItemRepository).delete(any(OrderItem.class));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        // When
        orderService.removeItem(1L);

        // Then
        verify(orderItemRepository).findById(1L);
        verify(orderItemRepository).delete(testOrderItem);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("移除項目 - 移除最後項目總金額歸零")
    void removeItem_LastItem_OrderTotalZero() {
        // Given
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testOrderItem));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        doNothing().when(orderItemRepository).delete(any(OrderItem.class));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            return order;
        });

        // When
        orderService.removeItem(1L);

        // Then
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("移除項目 - 項目不存在拋出例外")
    void removeItem_NotFound_ThrowsException() {
        // Given
        when(orderItemRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.removeItem(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("訂單項目不存在");

        verify(orderItemRepository).findById(999L);
        verify(orderItemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("移除項目 - 訂單不存在拋出例外")
    void removeItem_OrderNotFound_ThrowsException() {
        // Given
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testOrderItem));
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.removeItem(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("訂單不存在");

        verify(orderItemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("移除項目 - 非草稿訂單拋出例外")
    void removeItem_OrderNotDraft_ThrowsException() {
        // Given
        OrderItem itemInCompletedOrder = OrderItem.builder()
                .id(10L)
                .orderId(2L)
                .productId(1L)
                .productName("招牌漢堡")
                .unitPrice(new BigDecimal("59.00"))
                .quantity(1)
                .subtotal(new BigDecimal("59.00"))
                .optionsAmount(BigDecimal.ZERO)
                .build();

        when(orderItemRepository.findById(10L)).thenReturn(Optional.of(itemInCompletedOrder));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(completedOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.removeItem(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("只有草稿狀態的訂單可以刪除項目");

        verify(orderItemRepository, never()).delete(any());
    }

    // ========== 選項驗證測試 ==========

    @Test
    @DisplayName("加入商品 - 有效選項驗證成功")
    void addItem_WithValidOptions_Success() throws JsonProcessingException {
        // Given
        ProductOptionGroup group = ProductOptionGroup.builder()
                .id(1L)
                .productId(1L)
                .name("加料")
                .isActive(true)
                .build();

        ProductOptionValue value = ProductOptionValue.builder()
                .id(1L)
                .groupId(1L)
                .name("加起司")
                .priceAdjustment(new BigDecimal("10.00"))
                .isActive(true)
                .build();

        List<OrderItemOptionDTO> options = List.of(
                OrderItemOptionDTO.builder()
                        .groupName("加料")
                        .valueName("加起司")
                        .priceAdjustment(new BigDecimal("10.00"))
                        .build()
        );

        AddItemRequest request = AddItemRequest.builder()
                .orderId(1L)
                .productId(1L)
                .quantity(1)
                .options(options)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(optionGroupRepository.findByProductIdAndIsActiveOrderBySortOrder(1L, true))
                .thenReturn(List.of(group));
        when(optionValueRepository.findByGroupIdInOrderByGroupIdAndSortOrder(List.of(1L)))
                .thenReturn(List.of(value));
        when(objectMapper.writeValueAsString(any())).thenReturn("[{}]");
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            item.setId(1L);
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            return item;
        });
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        // When
        OrderItemDTO result = orderService.addItem(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOptionsAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        verify(optionGroupRepository).findByProductIdAndIsActiveOrderBySortOrder(1L, true);
        verify(optionValueRepository).findByGroupIdInOrderByGroupIdAndSortOrder(List.of(1L));
    }

    @Test
    @DisplayName("加入商品 - 無效選項群組名稱拋出例外")
    void addItem_WithInvalidGroupName_ThrowsException() {
        // Given
        ProductOptionGroup group = ProductOptionGroup.builder()
                .id(1L)
                .productId(1L)
                .name("加料")
                .isActive(true)
                .build();

        List<OrderItemOptionDTO> options = List.of(
                OrderItemOptionDTO.builder()
                        .groupName("不存在的群組")
                        .valueName("加起司")
                        .priceAdjustment(new BigDecimal("10.00"))
                        .build()
        );

        AddItemRequest request = AddItemRequest.builder()
                .orderId(1L)
                .productId(1L)
                .quantity(1)
                .options(options)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(optionGroupRepository.findByProductIdAndIsActiveOrderBySortOrder(1L, true))
                .thenReturn(List.of(group));

        // When & Then
        assertThatThrownBy(() -> orderService.addItem(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("選項群組不存在: 不存在的群組");

        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("加入商品 - 無效選項值名稱拋出例外")
    void addItem_WithInvalidValueName_ThrowsException() {
        // Given
        ProductOptionGroup group = ProductOptionGroup.builder()
                .id(1L)
                .productId(1L)
                .name("加料")
                .isActive(true)
                .build();

        ProductOptionValue value = ProductOptionValue.builder()
                .id(1L)
                .groupId(1L)
                .name("加起司")
                .priceAdjustment(new BigDecimal("10.00"))
                .isActive(true)
                .build();

        List<OrderItemOptionDTO> options = List.of(
                OrderItemOptionDTO.builder()
                        .groupName("加料")
                        .valueName("不存在的選項")
                        .priceAdjustment(new BigDecimal("10.00"))
                        .build()
        );

        AddItemRequest request = AddItemRequest.builder()
                .orderId(1L)
                .productId(1L)
                .quantity(1)
                .options(options)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(optionGroupRepository.findByProductIdAndIsActiveOrderBySortOrder(1L, true))
                .thenReturn(List.of(group));
        when(optionValueRepository.findByGroupIdInOrderByGroupIdAndSortOrder(List.of(1L)))
                .thenReturn(List.of(value));

        // When & Then
        assertThatThrownBy(() -> orderService.addItem(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("選項值不存在: 不存在的選項");

        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("加入商品 - 商品無選項但傳入選項拋出例外")
    void addItem_ProductHasNoOptions_ThrowsException() {
        // Given
        List<OrderItemOptionDTO> options = List.of(
                OrderItemOptionDTO.builder()
                        .groupName("加料")
                        .valueName("加起司")
                        .priceAdjustment(new BigDecimal("10.00"))
                        .build()
        );

        AddItemRequest request = AddItemRequest.builder()
                .orderId(1L)
                .productId(1L)
                .quantity(1)
                .options(options)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(optionGroupRepository.findByProductIdAndIsActiveOrderBySortOrder(1L, true))
                .thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> orderService.addItem(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("此商品沒有可用的選項");

        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("加入商品 - 價格不一致使用資料庫價格")
    void addItem_WithMismatchedPrice_UsesDbPrice() throws JsonProcessingException {
        // Given
        ProductOptionGroup group = ProductOptionGroup.builder()
                .id(1L)
                .productId(1L)
                .name("加料")
                .isActive(true)
                .build();

        ProductOptionValue value = ProductOptionValue.builder()
                .id(1L)
                .groupId(1L)
                .name("加起司")
                .priceAdjustment(new BigDecimal("10.00"))  // 資料庫價格
                .isActive(true)
                .build();

        List<OrderItemOptionDTO> options = List.of(
                OrderItemOptionDTO.builder()
                        .groupName("加料")
                        .valueName("加起司")
                        .priceAdjustment(new BigDecimal("5.00"))  // 客戶端傳入錯誤價格
                        .build()
        );

        AddItemRequest request = AddItemRequest.builder()
                .orderId(1L)
                .productId(1L)
                .quantity(1)
                .options(options)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(optionGroupRepository.findByProductIdAndIsActiveOrderBySortOrder(1L, true))
                .thenReturn(List.of(group));
        when(optionValueRepository.findByGroupIdInOrderByGroupIdAndSortOrder(List.of(1L)))
                .thenReturn(List.of(value));
        when(objectMapper.writeValueAsString(any())).thenReturn("[{}]");
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            item.setId(1L);
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            return item;
        });
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Collections.emptyList());

        // When
        OrderItemDTO result = orderService.addItem(request);

        // Then - 應使用資料庫價格 10.00，而非客戶端傳入的 5.00
        assertThat(result).isNotNull();
        assertThat(result.getOptionsAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        // 小計: (59.00 + 10.00) * 1 = 69.00
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("69.00"));
    }

    @Test
    @DisplayName("更新項目 - 有效選項驗證成功")
    void updateItem_WithValidOptions_Success() throws JsonProcessingException {
        // Given
        ProductOptionGroup group = ProductOptionGroup.builder()
                .id(1L)
                .productId(1L)
                .name("加料")
                .isActive(true)
                .build();

        ProductOptionValue value = ProductOptionValue.builder()
                .id(1L)
                .groupId(1L)
                .name("加蛋")
                .priceAdjustment(new BigDecimal("15.00"))
                .isActive(true)
                .build();

        List<OrderItemOptionDTO> options = List.of(
                OrderItemOptionDTO.builder()
                        .groupName("加料")
                        .valueName("加蛋")
                        .priceAdjustment(new BigDecimal("15.00"))
                        .build()
        );

        UpdateItemRequest request = UpdateItemRequest.builder()
                .itemId(1L)
                .options(options)
                .build();

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testOrderItem));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(optionGroupRepository.findByProductIdAndIsActiveOrderBySortOrder(1L, true))
                .thenReturn(List.of(group));
        when(optionValueRepository.findByGroupIdInOrderByGroupIdAndSortOrder(List.of(1L)))
                .thenReturn(List.of(value));
        when(objectMapper.writeValueAsString(any())).thenReturn("[{}]");
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(testOrderItem));

        // When
        OrderItemDTO result = orderService.updateItem(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOptionsAmount()).isEqualByComparingTo(new BigDecimal("15.00"));
        verify(optionGroupRepository).findByProductIdAndIsActiveOrderBySortOrder(1L, true);
        verify(optionValueRepository).findByGroupIdInOrderByGroupIdAndSortOrder(List.of(1L));
    }

    @Test
    @DisplayName("更新項目 - 無效選項群組拋出例外")
    void updateItem_WithInvalidOptionGroup_ThrowsException() {
        // Given
        ProductOptionGroup group = ProductOptionGroup.builder()
                .id(1L)
                .productId(1L)
                .name("加料")
                .isActive(true)
                .build();

        List<OrderItemOptionDTO> options = List.of(
                OrderItemOptionDTO.builder()
                        .groupName("無效群組")
                        .valueName("加蛋")
                        .priceAdjustment(new BigDecimal("15.00"))
                        .build()
        );

        UpdateItemRequest request = UpdateItemRequest.builder()
                .itemId(1L)
                .options(options)
                .build();

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testOrderItem));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(optionGroupRepository.findByProductIdAndIsActiveOrderBySortOrder(1L, true))
                .thenReturn(List.of(group));

        // When & Then
        assertThatThrownBy(() -> orderService.updateItem(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("選項群組不存在: 無效群組");

        verify(orderItemRepository, never()).save(any());
    }
}
