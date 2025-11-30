package com.morningharvest.erp.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.combo.entity.Combo;
import com.morningharvest.erp.combo.entity.ComboItem;
import com.morningharvest.erp.combo.repository.ComboItemRepository;
import com.morningharvest.erp.combo.repository.ComboRepository;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.order.dto.*;
import com.morningharvest.erp.order.entity.*;
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
import static org.mockito.ArgumentMatchers.*;
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
    private ComboRepository comboRepository;

    @Mock
    private ComboItemRepository comboItemRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private Order completedOrder;
    private Product testProduct;
    private Product inactiveProduct;

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
    }

    // ========== createOrder 測試 ==========

    @Test
    @DisplayName("建立訂單 - 空訂單成功")
    void createOrder_Empty_Success() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder().build();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(1L);
            o.setCreatedAt(LocalDateTime.now());
            o.setUpdatedAt(LocalDateTime.now());
            return o;
        });
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(1L)).thenReturn(Collections.emptyList());

        // When
        OrderDetailDTO result = orderService.createOrder(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("DRAFT");
        assertThat(result.getOrderType()).isEqualTo("DINE_IN");
        verify(orderRepository).save(any(Order.class)); // 空訂單只會 save 一次
    }

    @Test
    @DisplayName("建立訂單 - 含項目成功")
    void createOrder_WithItems_Success() throws JsonProcessingException {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .orderType("TAKEOUT")
                .note("外帶訂單")
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(1L)
                                .quantity(2)
                                .build()
                ))
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getId() == null) {
                o.setId(1L);
            }
            o.setCreatedAt(LocalDateTime.now());
            o.setUpdatedAt(LocalDateTime.now());
            return o;
        });
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            item.setId(1L);
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            return item;
        });

        SingleOrderItem savedItem = new SingleOrderItem();
        savedItem.setId(1L);
        savedItem.setOrderId(1L);
        savedItem.setProductId(1L);
        savedItem.setProductName("招牌漢堡");
        savedItem.setUnitPrice(new BigDecimal("59.00"));
        savedItem.setQuantity(2);
        savedItem.setSubtotal(new BigDecimal("118.00"));
        savedItem.setCreatedAt(LocalDateTime.now());
        savedItem.setUpdatedAt(LocalDateTime.now());

        when(orderItemRepository.findByOrderIdOrderByIdAsc(1L)).thenReturn(List.of(savedItem));

        Order orderWithAmount = Order.builder()
                .id(1L)
                .status("DRAFT")
                .orderType("TAKEOUT")
                .totalAmount(new BigDecimal("118.00"))
                .note("外帶訂單")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(orderWithAmount));

        // When
        OrderDetailDTO result = orderService.createOrder(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderType()).isEqualTo("TAKEOUT");
        assertThat(result.getNote()).isEqualTo("外帶訂單");
        assertThat(result.getItems()).hasSize(1);
        verify(orderItemRepository).save(any(SingleOrderItem.class));
    }

    // ========== getOrderById 測試 ==========

    @Test
    @DisplayName("查詢訂單 - 成功")
    void getOrderById_Success() {
        // Given
        SingleOrderItem testItem = new SingleOrderItem();
        testItem.setId(1L);
        testItem.setOrderId(1L);
        testItem.setProductId(1L);
        testItem.setProductName("招牌漢堡");
        testItem.setUnitPrice(new BigDecimal("59.00"));
        testItem.setQuantity(1);
        testItem.setSubtotal(new BigDecimal("59.00"));
        testItem.setCreatedAt(LocalDateTime.now());
        testItem.setUpdatedAt(LocalDateTime.now());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(1L)).thenReturn(List.of(testItem));

        // When
        OrderDetailDTO result = orderService.getOrderById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("DRAFT");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("招牌漢堡");
        verify(orderRepository).findById(1L);
        verify(orderItemRepository).findByOrderIdOrderByIdAsc(1L);
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

    // ========== updateOrder 測試 ==========

    @Test
    @DisplayName("更新訂單 - 成功")
    void updateOrder_Success() throws JsonProcessingException {
        // Given
        UpdateOrderRequest request = UpdateOrderRequest.builder()
                .orderType("TAKEOUT")
                .note("改外帶")
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(1L)
                                .quantity(3)
                                .build()
                ))
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(orderItemRepository).deleteByOrderId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            item.setId(1L);
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            return item;
        });

        SingleOrderItem savedItem = new SingleOrderItem();
        savedItem.setId(1L);
        savedItem.setOrderId(1L);
        savedItem.setProductId(1L);
        savedItem.setProductName("招牌漢堡");
        savedItem.setUnitPrice(new BigDecimal("59.00"));
        savedItem.setQuantity(3);
        savedItem.setSubtotal(new BigDecimal("177.00"));
        savedItem.setCreatedAt(LocalDateTime.now());
        savedItem.setUpdatedAt(LocalDateTime.now());

        when(orderItemRepository.findByOrderIdOrderByIdAsc(1L)).thenReturn(List.of(savedItem));

        // When
        OrderDetailDTO result = orderService.updateOrder(1L, request);

        // Then
        assertThat(result).isNotNull();
        verify(orderItemRepository).deleteByOrderId(1L);
        verify(orderItemRepository).save(any(SingleOrderItem.class));
    }

    @Test
    @DisplayName("更新訂單 - 訂單不存在拋出例外")
    void updateOrder_NotFound_ThrowsException() {
        // Given
        UpdateOrderRequest request = UpdateOrderRequest.builder()
                .items(List.of())
                .build();
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrder(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("訂單不存在");
    }

    @Test
    @DisplayName("更新訂單 - 非草稿狀態拋出例外")
    void updateOrder_NotDraft_ThrowsException() {
        // Given
        UpdateOrderRequest request = UpdateOrderRequest.builder()
                .items(List.of())
                .build();
        when(orderRepository.findById(2L)).thenReturn(Optional.of(completedOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrder(2L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("只有草稿狀態的訂單可以操作");
    }

    // ========== deleteOrder 測試 ==========

    @Test
    @DisplayName("刪除訂單 - 成功")
    void deleteOrder_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        doNothing().when(orderItemRepository).deleteByOrderId(1L);
        doNothing().when(orderRepository).delete(any(Order.class));

        // When
        orderService.deleteOrder(1L);

        // Then
        verify(orderItemRepository).deleteByOrderId(1L);
        verify(orderRepository).delete(testOrder);
    }

    @Test
    @DisplayName("刪除訂單 - 訂單不存在拋出例外")
    void deleteOrder_NotFound_ThrowsException() {
        // Given
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.deleteOrder(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("訂單不存在");
    }

    @Test
    @DisplayName("刪除訂單 - 非草稿狀態拋出例外")
    void deleteOrder_NotDraft_ThrowsException() {
        // Given
        when(orderRepository.findById(2L)).thenReturn(Optional.of(completedOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.deleteOrder(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("只有草稿狀態的訂單可以操作");
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
                .hasMessageContaining("只有草稿狀態的訂單可以操作");

        verify(orderRepository).findById(2L);
        verify(orderRepository, never()).save(any());
    }

    // ========== 選項驗證測試 ==========

    @Test
    @DisplayName("建立訂單 - 含選項成功")
    void createOrder_WithOptions_Success() throws JsonProcessingException {
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

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(1L)
                                .quantity(1)
                                .options(options)
                                .build()
                ))
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getId() == null) {
                o.setId(1L);
            }
            return o;
        });
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

        SingleOrderItem savedItem = new SingleOrderItem();
        savedItem.setId(1L);
        savedItem.setOrderId(1L);
        savedItem.setOptionsAmount(new BigDecimal("10.00"));
        savedItem.setSubtotal(new BigDecimal("69.00"));
        savedItem.setCreatedAt(LocalDateTime.now());
        savedItem.setUpdatedAt(LocalDateTime.now());

        when(orderItemRepository.findByOrderIdOrderByIdAsc(1L)).thenReturn(List.of(savedItem));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        OrderDetailDTO result = orderService.createOrder(request);

        // Then
        assertThat(result).isNotNull();
        verify(optionGroupRepository).findByProductIdAndIsActiveOrderBySortOrder(1L, true);
        verify(optionValueRepository).findByGroupIdInOrderByGroupIdAndSortOrder(List.of(1L));
    }

    @Test
    @DisplayName("建立訂單 - 無效選項群組拋出例外")
    void createOrder_WithInvalidOptionGroup_ThrowsException() {
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

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("SINGLE")
                                .productId(1L)
                                .quantity(1)
                                .options(options)
                                .build()
                ))
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(1L);
            return o;
        });
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(optionGroupRepository.findByProductIdAndIsActiveOrderBySortOrder(1L, true))
                .thenReturn(List.of(group));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("選項群組不存在: 不存在的群組");
    }

    // ========== 套餐相關測試 ==========

    @Test
    @DisplayName("建立訂單 - 含套餐成功")
    void createOrder_WithCombo_Success() throws JsonProcessingException {
        // Given
        Combo combo = Combo.builder()
                .id(1L)
                .name("經典套餐")
                .price(new BigDecimal("199.00"))
                .isActive(true)
                .build();

        Product product1 = Product.builder()
                .id(10L)
                .name("漢堡")
                .price(new BigDecimal("89.00"))
                .isActive(true)
                .build();

        ComboItem comboItem1 = ComboItem.builder()
                .id(1L)
                .comboId(1L)
                .productId(10L)
                .productName("漢堡")
                .quantity(1)
                .sortOrder(1)
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("COMBO")
                                .comboId(1L)
                                .build()
                ))
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getId() == null) {
                o.setId(1L);
            }
            return o;
        });
        when(comboRepository.findById(1L)).thenReturn(Optional.of(combo));
        when(comboItemRepository.findByComboIdOrderBySortOrder(1L)).thenReturn(List.of(comboItem1));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product1));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            item.setId(System.nanoTime());
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            return item;
        });

        ComboOrderItem comboHeader = new ComboOrderItem();
        comboHeader.setId(1L);
        comboHeader.setOrderId(1L);
        comboHeader.setComboId(1L);
        comboHeader.setComboName("經典套餐");
        comboHeader.setComboPrice(new BigDecimal("199.00"));
        comboHeader.setSubtotal(new BigDecimal("199.00"));
        comboHeader.setGroupSequence(1);
        comboHeader.setCreatedAt(LocalDateTime.now());
        comboHeader.setUpdatedAt(LocalDateTime.now());

        ComboItemOrderItem comboItemOrder = new ComboItemOrderItem();
        comboItemOrder.setId(2L);
        comboItemOrder.setOrderId(1L);
        comboItemOrder.setComboId(1L);
        comboItemOrder.setGroupSequence(1);
        comboItemOrder.setProductId(10L);
        comboItemOrder.setProductName("漢堡");
        comboItemOrder.setQuantity(1);
        comboItemOrder.setSubtotal(BigDecimal.ZERO);
        comboItemOrder.setCreatedAt(LocalDateTime.now());
        comboItemOrder.setUpdatedAt(LocalDateTime.now());

        when(orderItemRepository.findByOrderIdOrderByIdAsc(1L)).thenReturn(List.of(comboHeader, comboItemOrder));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        OrderDetailDTO result = orderService.createOrder(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(2); // 1 COMBO + 1 COMBO_ITEM
        verify(comboRepository).findById(1L);
        verify(comboItemRepository).findByComboIdOrderBySortOrder(1L);
        verify(orderItemRepository, times(2)).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("建立訂單 - 套餐不存在拋出例外")
    void createOrder_ComboNotFound_ThrowsException() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("COMBO")
                                .comboId(999L)
                                .build()
                ))
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(1L);
            return o;
        });
        when(comboRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("套餐不存在");
    }

    @Test
    @DisplayName("建立訂單 - 套餐已停用拋出例外")
    void createOrder_ComboInactive_ThrowsException() {
        // Given
        Combo inactiveCombo = Combo.builder()
                .id(1L)
                .name("停用套餐")
                .price(new BigDecimal("199.00"))
                .isActive(false)
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder()
                                .type("COMBO")
                                .comboId(1L)
                                .build()
                ))
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(1L);
            return o;
        });
        when(comboRepository.findById(1L)).thenReturn(Optional.of(inactiveCombo));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("套餐已停用");
    }
}
