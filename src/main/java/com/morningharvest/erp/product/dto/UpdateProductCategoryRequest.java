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
public class UpdateProductCategoryRequest {

    @NotNull(message = "分類 ID 不可為空")
    private Long id;

    @NotBlank(message = "分類名稱不可為空")
    @Size(max = 50, message = "分類名稱不可超過 50 字元")
    private String name;

    @Size(max = 200, message = "分類說明不可超過 200 字元")
    private String description;

    @Min(value = 0, message = "排序順序不可為負數")
    private Integer sortOrder;
}
