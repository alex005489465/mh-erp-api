package com.morningharvest.erp.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 訂單項目請求 DTO
 * 用於建立或更新訂單時的項目資料
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {

    @NotNull(message = "項目類型不能為空")
    @Pattern(regexp = "^(SINGLE|COMBO)$", message = "項目類型必須為 SINGLE 或 COMBO")
    private String type;

    // === 單點商品欄位 (type = SINGLE) ===

    /**
     * 商品 ID（單點時必填）
     */
    private Long productId;

    /**
     * 數量（單點時使用）
     */
    @Min(value = 1, message = "數量至少為 1")
    @Builder.Default
    private Integer quantity = 1;

    /**
     * 商品選項（單點時使用）
     */
    @Valid
    private List<OrderItemOptionDTO> options;

    /**
     * 備註
     */
    private String note;

    // === 套餐欄位 (type = COMBO) ===

    /**
     * 套餐 ID（套餐時必填）
     */
    private Long comboId;

    /**
     * 套餐內各商品的選項配置（套餐時使用）
     */
    @Valid
    private List<ComboItemOptionsDTO> comboItemOptions;
}
