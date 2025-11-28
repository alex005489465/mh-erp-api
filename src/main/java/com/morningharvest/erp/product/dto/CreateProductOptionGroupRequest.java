package com.morningharvest.erp.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductOptionGroupRequest {

    @NotNull(message = "產品 ID 不可為空")
    private Long productId;

    @NotBlank(message = "群組名稱不可為空")
    @Size(max = 50, message = "群組名稱不可超過 50 字元")
    private String name;

    @NotNull(message = "最少選擇數不可為空")
    @Min(value = 0, message = "最少選擇數不可為負數")
    private Integer minSelections;

    @NotNull(message = "最多選擇數不可為空")
    @Min(value = 1, message = "最多選擇數至少為 1")
    private Integer maxSelections;

    @Min(value = 0, message = "排序順序不可為負數")
    private Integer sortOrder;
}
