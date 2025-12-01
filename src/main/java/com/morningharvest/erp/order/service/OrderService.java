package com.morningharvest.erp.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.combo.entity.Combo;
import com.morningharvest.erp.combo.entity.ComboItem;
import com.morningharvest.erp.combo.repository.ComboItemRepository;
import com.morningharvest.erp.combo.repository.ComboRepository;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.event.EventPublisher;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.order.dto.*;
import com.morningharvest.erp.order.event.OrderSubmittedEvent;
import com.morningharvest.erp.order.entity.*;
import com.morningharvest.erp.order.repository.OrderItemRepository;
import com.morningharvest.erp.order.repository.OrderRepository;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.entity.ProductOptionGroup;
import com.morningharvest.erp.product.entity.ProductOptionValue;
import com.morningharvest.erp.product.repository.ProductOptionGroupRepository;
import com.morningharvest.erp.product.repository.ProductOptionValueRepository;
import com.morningharvest.erp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductOptionGroupRepository optionGroupRepository;
    private final ProductOptionValueRepository optionValueRepository;
    private final ComboRepository comboRepository;
    private final ComboItemRepository comboItemRepository;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;

    /**
     * 建立訂單（含項目）
     */
    @Transactional
    public OrderDetailDTO createOrder(CreateOrderRequest request) {
        log.info("建立訂單, orderType: {}", request.getOrderType());

        Order order = Order.builder()
                .status("DRAFT")
                .orderType(request.getOrderType() != null ? request.getOrderType() : "DINE_IN")
                .totalAmount(BigDecimal.ZERO)
                .note(request.getNote())
                .build();

        Order saved = orderRepository.save(order);
        log.info("訂單建立成功, id: {}", saved.getId());

        // 建立訂單項目
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            createOrderItems(saved.getId(), request.getItems());
            recalculateOrderTotal(saved);
        }

        return getOrderById(saved.getId());
    }

    /**
     * 取得訂單詳情（含項目）
     */
    @Transactional(readOnly = true)
    public OrderDetailDTO getOrderById(Long id) {
        log.debug("查詢訂單, id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在: " + id));

        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(id);
        List<OrderItemDTO> itemDTOs = items.stream()
                .map(OrderItemDTO::from)
                .toList();

        return OrderDetailDTO.from(order, itemDTOs);
    }

    /**
     * 查詢訂單列表（分頁）
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderDTO> listOrders(PageableRequest pageableRequest, String status, String orderType) {
        log.debug("查詢訂單列表, page: {}, size: {}, status: {}, orderType: {}",
                pageableRequest.getPage(), pageableRequest.getSize(), status, orderType);

        Page<Order> orderPage;

        if (status != null && orderType != null) {
            orderPage = orderRepository.findByStatusAndOrderType(status, orderType, pageableRequest.toPageable());
        } else if (status != null) {
            orderPage = orderRepository.findByStatus(status, pageableRequest.toPageable());
        } else if (orderType != null) {
            orderPage = orderRepository.findByOrderType(orderType, pageableRequest.toPageable());
        } else {
            orderPage = orderRepository.findAll(pageableRequest.toPageable());
        }

        Page<OrderDTO> dtoPage = orderPage.map(OrderDTO::from);
        return PageResponse.from(dtoPage);
    }

    /**
     * 更新訂單（整批取代項目）
     */
    @Transactional
    public OrderDetailDTO updateOrder(Long orderId, UpdateOrderRequest request) {
        log.info("更新訂單, orderId: {}", orderId);

        Order order = findDraftOrder(orderId);

        // 更新訂單基本資訊
        if (request.getOrderType() != null) {
            order.setOrderType(request.getOrderType());
        }
        if (request.getNote() != null) {
            order.setNote(request.getNote());
        }
        orderRepository.save(order);

        // 刪除所有舊項目
        orderItemRepository.deleteByOrderId(orderId);

        // 建立所有新項目
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            createOrderItems(orderId, request.getItems());
        }

        // 重新計算總金額
        recalculateOrderTotal(order);

        log.info("訂單更新成功, orderId: {}", orderId);
        return getOrderById(orderId);
    }

    /**
     * 刪除訂單
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        log.info("刪除訂單, orderId: {}", orderId);

        Order order = findDraftOrder(orderId);

        orderItemRepository.deleteByOrderId(orderId);
        orderRepository.delete(order);

        log.info("訂單刪除成功, orderId: {}", orderId);
    }

    /**
     * 送出訂單（DRAFT → PENDING_PAYMENT）
     */
    @Transactional
    public OrderDTO submitOrder(Long id) {
        log.info("送出訂單, id: {}", id);

        Order order = findDraftOrder(id);

        order.setStatus("PENDING_PAYMENT");
        Order saved = orderRepository.save(order);
        log.info("訂單已送出, id: {}", saved.getId());

        // 發布訂單送出事件
        eventPublisher.publish(
                new OrderSubmittedEvent(saved.getId(), saved.getTotalAmount()),
                "訂單送出"
        );

        return OrderDTO.from(saved);
    }

    /**
     * 完成訂單（PAID → COMPLETED）
     */
    @Transactional
    public OrderDTO completeOrder(Long id) {
        log.info("完成訂單, id: {}", id);

        Order order = findPaidOrder(id);

        order.setStatus("COMPLETED");
        Order saved = orderRepository.save(order);
        log.info("訂單完成, id: {}", saved.getId());

        return OrderDTO.from(saved);
    }

    // ========== 內部方法 ==========

    /**
     * 取得草稿狀態的訂單
     */
    private Order findDraftOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在: " + orderId));

        if (!"DRAFT".equals(order.getStatus())) {
            throw new IllegalStateException("只有草稿狀態的訂單可以操作");
        }

        return order;
    }

    /**
     * 取得已付款狀態的訂單
     */
    private Order findPaidOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在: " + orderId));

        if (!"PAID".equals(order.getStatus())) {
            throw new IllegalStateException("只有已付款狀態的訂單可以完成");
        }

        return order;
    }

    /**
     * 建立訂單項目
     */
    private void createOrderItems(Long orderId, List<OrderItemRequest> items) {
        int groupSequence = 1;

        for (OrderItemRequest item : items) {
            if ("SINGLE".equals(item.getType())) {
                createSingleItem(orderId, item, groupSequence);
                groupSequence++;
            } else if ("COMBO".equals(item.getType())) {
                createComboItems(orderId, item, groupSequence);
                groupSequence++;
            } else {
                throw new IllegalArgumentException("未知的項目類型: " + item.getType());
            }
        }
    }

    /**
     * 建立單點商品項目
     */
    private void createSingleItem(Long orderId, OrderItemRequest request, int groupSequence) {
        log.debug("建立單點商品, orderId: {}, productId: {}", orderId, request.getProductId());

        if (request.getProductId() == null) {
            throw new IllegalArgumentException("單點商品必須指定 productId");
        }

        // 取得商品資訊
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在: " + request.getProductId()));

        if (!product.getIsActive()) {
            throw new IllegalArgumentException("商品已下架: " + product.getName());
        }

        // 驗證並處理選項
        List<OrderItemOptionDTO> validatedOptions = validateAndProcessOptions(
                product.getId(), product.getName(), request.getOptions());

        // 計算選項加價
        BigDecimal optionsAmount = calculateOptionsAmount(validatedOptions);

        // 建立訂單項目
        SingleOrderItem item = new SingleOrderItem();
        item.setOrderId(orderId);
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setUnitPrice(product.getPrice());
        item.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        item.setOptions(serializeOptions(validatedOptions));
        item.setOptionsAmount(optionsAmount);
        item.setNote(request.getNote());

        item.calculateSubtotal();
        orderItemRepository.save(item);
    }

    /**
     * 建立套餐項目（標題 + 內容）
     */
    private void createComboItems(Long orderId, OrderItemRequest request, int groupSequence) {
        log.debug("建立套餐, orderId: {}, comboId: {}", orderId, request.getComboId());

        if (request.getComboId() == null) {
            throw new IllegalArgumentException("套餐必須指定 comboId");
        }

        // 驗證套餐存在且已啟用
        Combo combo = comboRepository.findById(request.getComboId())
                .orElseThrow(() -> new ResourceNotFoundException("套餐不存在: " + request.getComboId()));

        if (!combo.getIsActive()) {
            throw new IllegalArgumentException("套餐已停用: " + combo.getName());
        }

        // 取得套餐項目列表
        List<ComboItem> comboItems = comboItemRepository.findByComboIdOrderBySortOrder(request.getComboId());
        if (comboItems.isEmpty()) {
            throw new IllegalArgumentException("套餐沒有包含任何商品: " + combo.getName());
        }

        // 建立選項對應表
        Map<Long, ComboItemOptionsDTO> optionsMap = buildComboItemOptionsMap(request.getComboItemOptions());

        // 1. 建立套餐標題行 (COMBO)
        ComboOrderItem comboHeader = new ComboOrderItem();
        comboHeader.setOrderId(orderId);
        comboHeader.setComboId(combo.getId());
        comboHeader.setComboName(combo.getName());
        comboHeader.setComboPrice(combo.getPrice());
        comboHeader.setGroupSequence(groupSequence);
        comboHeader.calculateSubtotal();
        orderItemRepository.save(comboHeader);

        // 2. 為每個套餐項目建立 COMBO_ITEM
        for (ComboItem comboItem : comboItems) {
            // 驗證商品存在且已上架
            Product product = productRepository.findById(comboItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("套餐內商品不存在: " + comboItem.getProductId()));

            if (!product.getIsActive()) {
                throw new IllegalArgumentException("套餐內商品已下架: " + product.getName());
            }

            // 取得該商品的選項配置
            ComboItemOptionsDTO itemOptions = optionsMap.get(comboItem.getProductId());
            List<OrderItemOptionDTO> validatedOptions = null;
            BigDecimal optionsAmount = BigDecimal.ZERO;

            if (itemOptions != null && itemOptions.getOptions() != null && !itemOptions.getOptions().isEmpty()) {
                validatedOptions = validateAndProcessOptions(
                        product.getId(), product.getName(), itemOptions.getOptions());
                optionsAmount = calculateOptionsAmount(validatedOptions);
            }

            // 建立 COMBO_ITEM
            ComboItemOrderItem orderItem = new ComboItemOrderItem();
            orderItem.setOrderId(orderId);
            orderItem.setComboId(combo.getId());
            orderItem.setGroupSequence(groupSequence);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(comboItem.getQuantity());
            orderItem.setOptions(serializeOptions(validatedOptions));
            orderItem.setOptionsAmount(optionsAmount);
            orderItem.setNote(itemOptions != null ? itemOptions.getNote() : null);

            orderItem.calculateSubtotal();
            orderItemRepository.save(orderItem);
        }
    }

    /**
     * 建立套餐商品選項對應表
     */
    private Map<Long, ComboItemOptionsDTO> buildComboItemOptionsMap(List<ComboItemOptionsDTO> comboItemOptions) {
        if (comboItemOptions == null || comboItemOptions.isEmpty()) {
            return Map.of();
        }
        return comboItemOptions.stream()
                .filter(opt -> opt.getProductId() != null)
                .collect(Collectors.toMap(ComboItemOptionsDTO::getProductId, opt -> opt, (a, b) -> a));
    }

    /**
     * 重新計算訂單總金額
     */
    private void recalculateOrderTotal(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());

        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);
        orderRepository.save(order);
        log.debug("訂單總金額更新, orderId: {}, total: {}", order.getId(), total);
    }

    /**
     * 計算選項加價總額
     */
    private BigDecimal calculateOptionsAmount(List<OrderItemOptionDTO> options) {
        if (options == null || options.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return options.stream()
                .map(opt -> opt.getPriceAdjustment() != null ? opt.getPriceAdjustment() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 序列化選項為 JSON
     */
    private String serializeOptions(List<OrderItemOptionDTO> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            log.error("選項序列化失敗", e);
            return null;
        }
    }

    /**
     * 驗證並處理訂單選項
     */
    private List<OrderItemOptionDTO> validateAndProcessOptions(Long productId, String productName, List<OrderItemOptionDTO> options) {
        if (options == null || options.isEmpty()) {
            return options;
        }

        // 查詢該產品的所有活躍選項群組
        List<ProductOptionGroup> groups = optionGroupRepository.findByProductIdAndIsActiveOrderBySortOrder(productId, true);
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("此商品沒有可用的選項: " + productName);
        }

        // 建立群組名稱到群組的 Map
        Map<String, ProductOptionGroup> groupMap = groups.stream()
                .collect(Collectors.toMap(ProductOptionGroup::getName, g -> g, (a, b) -> a));

        // 批量查詢所有選項值
        List<Long> groupIds = groups.stream().map(ProductOptionGroup::getId).toList();
        List<ProductOptionValue> allValues = optionValueRepository.findByGroupIdInOrderByGroupIdAndSortOrder(groupIds);

        // 建立 groupId -> (valueName -> value) 的 Map
        Map<Long, Map<String, ProductOptionValue>> valueMapByGroup = allValues.stream()
                .filter(ProductOptionValue::getIsActive)
                .collect(Collectors.groupingBy(
                        ProductOptionValue::getGroupId,
                        Collectors.toMap(ProductOptionValue::getName, v -> v, (a, b) -> a)
                ));

        // 驗證並處理每個選項
        List<OrderItemOptionDTO> validatedOptions = new ArrayList<>();
        for (OrderItemOptionDTO option : options) {
            // 驗證群組存在
            ProductOptionGroup group = groupMap.get(option.getGroupName());
            if (group == null) {
                throw new IllegalArgumentException(
                        String.format("選項群組不存在: %s (商品: %s)", option.getGroupName(), productName));
            }

            // 驗證選項值存在
            Map<String, ProductOptionValue> valueMap = valueMapByGroup.get(group.getId());
            if (valueMap == null) {
                throw new IllegalArgumentException(
                        String.format("選項群組沒有可用的選項值: %s (商品: %s)", option.getGroupName(), productName));
            }

            ProductOptionValue value = valueMap.get(option.getValueName());
            if (value == null) {
                throw new IllegalArgumentException(
                        String.format("選項值不存在: %s (群組: %s)", option.getValueName(), option.getGroupName()));
            }

            // 檢查價格是否一致
            if (option.getPriceAdjustment() != null &&
                    option.getPriceAdjustment().compareTo(value.getPriceAdjustment()) != 0) {
                log.warn("選項價格不一致, 客戶端: {}, 資料庫: {}, 群組: {}, 選項: {}",
                        option.getPriceAdjustment(), value.getPriceAdjustment(),
                        option.getGroupName(), option.getValueName());
            }

            // 使用資料庫價格建立驗證後的選項
            validatedOptions.add(OrderItemOptionDTO.builder()
                    .groupName(group.getName())
                    .valueName(value.getName())
                    .priceAdjustment(value.getPriceAdjustment())
                    .build());
        }

        return validatedOptions;
    }
}
