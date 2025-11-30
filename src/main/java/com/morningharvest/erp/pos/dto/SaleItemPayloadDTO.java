package com.morningharvest.erp.pos.dto;

import com.morningharvest.erp.order.dto.OrderItemOptionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 訂單項目 Payload DTO
 * 前端可直接將此物件放入訂單的 items 陣列
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleItemPayloadDTO {

    /**
     * 項目類型：SINGLE, COMBO, COMBO_ITEM
     */
    private String type;

    // === 單點商品欄位 (type = SINGLE) ===

    private Long productId;
    private String productName;
    private BigDecimal unitPrice;

    // === 套餐標頭欄位 (type = COMBO) ===

    private Long comboId;
    private String comboName;
    private BigDecimal comboPrice;

    // === 共用欄位 ===

    @Builder.Default
    private Integer quantity = 1;

    @Builder.Default
    private List<OrderItemOptionDTO> options = new ArrayList<>();

    /**
     * 建立單點商品 Payload
     */
    public static SaleItemPayloadDTO single(Long productId, String productName, BigDecimal unitPrice) {
        return SaleItemPayloadDTO.builder()
                .type("SINGLE")
                .productId(productId)
                .productName(productName)
                .unitPrice(unitPrice)
                .quantity(1)
                .options(new ArrayList<>())
                .build();
    }

    /**
     * 建立套餐標頭 Payload
     */
    public static SaleItemPayloadDTO comboHeader(Long comboId, String comboName, BigDecimal comboPrice) {
        return SaleItemPayloadDTO.builder()
                .type("COMBO")
                .comboId(comboId)
                .comboName(comboName)
                .comboPrice(comboPrice)
                .build();
    }

    /**
     * 建立套餐項目 Payload
     */
    public static SaleItemPayloadDTO comboItem(Long comboId, Long productId, String productName, Integer quantity) {
        return SaleItemPayloadDTO.builder()
                .type("COMBO_ITEM")
                .comboId(comboId)
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .options(new ArrayList<>())
                .build();
    }
}
