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
import com.morningharvest.erp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
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

        // 計算選項加價
        BigDecimal optionsAmount = calculateOptionsAmount(request.getOptions());

        // 建立訂單項目
        OrderItem item = OrderItem.builder()
                .orderId(request.getOrderId())
                .productId(product.getId())
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .options(serializeOptions(request.getOptions()))
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
            BigDecimal optionsAmount = calculateOptionsAmount(request.getOptions());
            item.setOptions(serializeOptions(request.getOptions()));
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
}
