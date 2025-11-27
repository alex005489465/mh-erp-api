package com.morningharvest.erp.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "商品名稱不可為空")
    @Size(max = 100, message = "商品名稱不可超過 100 字元")
    private String name;

    @Size(max = 500, message = "商品說明不可超過 500 字元")
    private String description;

    @NotNull(message = "商品價格不可為空")
    @DecimalMin(value = "0.00", inclusive = true, message = "商品價格不可為負數")
    private BigDecimal price;

    @Size(max = 500, message = "圖片網址不可超過 500 字元")
    private String imageUrl;

    private Long categoryId;

    @Size(max = 50, message = "分類名稱不可超過 50 字元")
    private String categoryName;

    @Min(value = 0, message = "排序順序不可為負數")
    private Integer sortOrder;
}
