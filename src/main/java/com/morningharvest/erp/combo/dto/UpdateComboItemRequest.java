package com.morningharvest.erp.combo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateComboItemRequest {

    @NotNull(message = "商品 ID 不可為空")
    private Long productId;

    @Min(value = 1, message = "數量至少為 1")
    private Integer quantity;

    @Min(value = 0, message = "排序順序不可為負數")
    private Integer sortOrder;
}
