package com.morningharvest.erp.order.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.order.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 訂單項目回應 DTO
 * 統一回傳格式，支援所有項目類型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Long id;
    private Long orderId;
    private String itemType;

    // 單點商品/套餐內商品欄位
    private Long productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private List<OrderItemOptionDTO> options;
    private BigDecimal optionsAmount;

    // 套餐欄位
    private Long comboId;
    private String comboName;
    private BigDecimal comboPrice;
    private Integer groupSequence;

    // 共用欄位
    private BigDecimal subtotal;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 從 Entity 轉換為 DTO（多型支援）
     */
    public static OrderItemDTO from(OrderItem item) {
        if (item instanceof SingleOrderItem single) {
            return fromSingle(single);
        } else if (item instanceof ComboOrderItem combo) {
            return fromCombo(combo);
        } else if (item instanceof ComboItemOrderItem comboItem) {
            return fromComboItem(comboItem);
        }
        throw new IllegalArgumentException("未知的訂單項目類型");
    }

    private static OrderItemDTO fromSingle(SingleOrderItem item) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .orderId(item.getOrderId())
                .itemType("SINGLE")
                .productId(item.getProductId())
                .productName(item.getProductName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .options(parseOptions(item.getOptions()))
                .optionsAmount(item.getOptionsAmount())
                .subtotal(item.getSubtotal())
                .note(item.getNote())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private static OrderItemDTO fromCombo(ComboOrderItem item) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .orderId(item.getOrderId())
                .itemType("COMBO")
                .comboId(item.getComboId())
                .comboName(item.getComboName())
                .comboPrice(item.getComboPrice())
                .groupSequence(item.getGroupSequence())
                .subtotal(item.getSubtotal())
                .note(item.getNote())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private static OrderItemDTO fromComboItem(ComboItemOrderItem item) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .orderId(item.getOrderId())
                .itemType("COMBO_ITEM")
                .comboId(item.getComboId())
                .groupSequence(item.getGroupSequence())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .options(parseOptions(item.getOptions()))
                .optionsAmount(item.getOptionsAmount())
                .subtotal(item.getSubtotal())
                .note(item.getNote())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private static List<OrderItemOptionDTO> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
