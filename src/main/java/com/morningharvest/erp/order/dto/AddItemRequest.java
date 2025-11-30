package com.morningharvest.erp.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 加入訂單項目請求
 * - 單點商品: 需設定 productId
 * - 套餐: 需設定 comboId
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddItemRequest {

    @NotNull(message = "訂單 ID 不可為空")
    private Long orderId;

    // === 單點商品欄位 ===

    /**
     * 單點商品時必填
     */
    private Long productId;

    @Min(value = 1, message = "數量至少為 1")
    @Builder.Default
    private Integer quantity = 1;

    /**
     * 單點商品的選項
     */
    private List<OrderItemOptionDTO> options;

    private String note;

    // === 套餐欄位 ===

    /**
     * 套餐時必填
     */
    private Long comboId;

    /**
     * 套餐內各商品的選項配置
     */
    private List<ComboItemOptionsDTO> comboItemOptions;
}
