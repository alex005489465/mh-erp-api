package com.morningharvest.erp.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 套餐內單一商品的選項配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComboItemOptionsDTO {

    @NotNull(message = "商品 ID 不可為空")
    private Long productId;

    private List<OrderItemOptionDTO> options;

    private String note;
}
