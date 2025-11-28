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
public class UpdateProductOptionValueRequest {

    @NotNull(message = "選項 ID 不可為空")
    private Long id;

    @NotBlank(message = "選項名稱不可為空")
    @Size(max = 50, message = "選項名稱不可超過 50 字元")
    private String name;

    @NotNull(message = "加價金額不可為空")
    @DecimalMin(value = "0.00", message = "加價金額不可為負數")
    private BigDecimal priceAdjustment;

    @Min(value = 0, message = "排序順序不可為負數")
    private Integer sortOrder;
}
