package com.morningharvest.erp.product.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class UpdateProductRecipeRequest {

    @NotNull(message = "配方項目 ID 不可為空")
    private Long id;

    @NotNull(message = "用量不可為空")
    @DecimalMin(value = "0.0001", inclusive = true, message = "用量必須大於 0")
    private BigDecimal quantity;

    @Size(max = 200, message = "備註不可超過 200 字元")
    private String note;
}
