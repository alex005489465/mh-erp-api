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
    private final ObjectMapper objectMapper;

    /**
     * 建立訂單（草稿狀態）
     */
    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        log.info("建立訂單, orderType: {}", request.getOrderType());

        Order order = Order.builder()
                .status("DRAFT")
                .orderType(request.getOrderType() != null ? request.getOrderType() : "DINE_IN")
                .totalAmount(BigDecimal.ZERO)
                .note(request.getNote())
                .build();

        Order saved = orderRepository.save(order);
        log.info("訂單建立成功, id: {}", saved.getId());

        return OrderDTO.from(saved);
    }

    /**
     * 取得訂單詳情（含項目）
     */
    @Transactional(readOnly = true)
    public OrderDetailDTO getOrderById(Long id) {
        log.debug("查詢訂單, id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在: " + id));

        List<OrderItem> items = orderItemRepository.findByOrderId(id);
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
     * 完成訂單
     */
    @Transactional
    public OrderDTO completeOrder(Long id) {
        log.info("完成訂單, id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在: " + id));

        if (!"DRAFT".equals(order.getStatus())) {
            throw new IllegalStateException("只有草稿狀態的訂單可以完成");
        }

        order.setStatus("COMPLETED");
        Order saved = orderRepository.save(order);
        log.info("訂單完成, id: {}", saved.getId());

        return OrderDTO.from(saved);
    }

    /**
     * 加入商品到訂單
     */
    @Transactional
    public OrderItemDTO addItem(AddItemRequest request) {
        log.info("加入商品到訂單, orderId: {}, productId: {}", request.getOrderId(), request.getProductId());

        // 驗證訂單存在且為草稿狀態
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在: " + request.getOrderId()));

        if (!"DRAFT".equals(order.getStatus())) {
            throw new IllegalStateException("只有草稿狀態的訂單可以新增項目");
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

        // 計算選項加價（使用驗證後的選項）
        BigDecimal optionsAmount = calculateOptionsAmount(validatedOptions);

        // 建立訂單項目
        OrderItem item = OrderItem.builder()
                .orderId(request.getOrderId())
                .productId(product.getId())
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .options(serializeOptions(validatedOptions))
                .optionsAmount(optionsAmount)
                .note(request.getNote())
                .build();

        item.calculateSubtotal();

        OrderItem saved = orderItemRepository.save(item);
        log.info("訂單項目新增成功, id: {}", saved.getId());

        // 重新計算訂單總金額
        recalculateOrderTotal(order);

        return OrderItemDTO.from(saved);
    }

    /**
     * 更新訂單項目
     */
    @Transactional
    public OrderItemDTO updateItem(UpdateItemRequest request) {
        log.info("更新訂單項目, itemId: {}", request.getItemId());

        OrderItem item = orderItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("訂單項目不存在: " + request.getItemId()));

        // 驗證訂單狀態
        Order order = orderRepository.findById(item.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在: " + item.getOrderId()));

        if (!"DRAFT".equals(order.getStatus())) {
            throw new IllegalStateException("只有草稿狀態的訂單可以修改項目");
        }

        // 更新數量
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }

        // 更新選項
        if (request.getOptions() != null) {
            // 取得商品資訊以驗證選項
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("商品不存在: " + item.getProductId()));

            List<OrderItemOptionDTO> validatedOptions = validateAndProcessOptions(
                    product.getId(), product.getName(), request.getOptions());

            BigDecimal optionsAmount = calculateOptionsAmount(validatedOptions);
            item.setOptions(serializeOptions(validatedOptions));
            item.setOptionsAmount(optionsAmount);
        }

        // 更新備註
        if (request.getNote() != null) {
            item.setNote(request.getNote());
        }

        item.calculateSubtotal();

        OrderItem saved = orderItemRepository.save(item);
        log.info("訂單項目更新成功, id: {}", saved.getId());

        // 重新計算訂單總金額
        recalculateOrderTotal(order);

        return OrderItemDTO.from(saved);
    }

    /**
     * 移除訂單項目
     */
    @Transactional
    public void removeItem(Long itemId) {
        log.info("移除訂單項目, itemId: {}", itemId);

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("訂單項目不存在: " + itemId));

        // 驗證訂單狀態
        Order order = orderRepository.findById(item.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在: " + item.getOrderId()));

        if (!"DRAFT".equals(order.getStatus())) {
            throw new IllegalStateException("只有草稿狀態的訂單可以刪除項目");
        }

        orderItemRepository.delete(item);
        log.info("訂單項目移除成功, itemId: {}", itemId);

        // 重新計算訂單總金額
        recalculateOrderTotal(order);
    }

    /**
     * 重新計算訂單總金額
     */
    private void recalculateOrderTotal(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

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
     * - 驗證選項群組和選項值是否存在於該產品
     * - 從資料庫取得正確的價格調整額
     *
     * @param productId   商品 ID
     * @param productName 商品名稱（用於錯誤訊息）
     * @param options     客戶端傳入的選項列表
     * @return 驗證後的選項列表（使用資料庫價格）
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

            // 檢查價格是否一致，不一致則記錄警告
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
