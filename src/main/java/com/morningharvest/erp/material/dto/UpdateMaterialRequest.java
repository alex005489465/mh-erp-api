package com.morningharvest.erp.material.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class UpdateMaterialRequest {

    @NotNull(message = "原物料 ID 不可為空")
    private Long id;

    @NotBlank(message = "原物料編號不可為空")
    @Size(max = 20, message = "原物料編號不可超過 20 字元")
    private String code;

    @NotBlank(message = "原物料名稱不可為空")
    @Size(max = 100, message = "原物料名稱不可超過 100 字元")
    private String name;

    @NotBlank(message = "單位不可為空")
    @Size(max = 20, message = "單位不可超過 20 字元")
    private String unit;

    @Size(max = 50, message = "分類不可超過 50 字元")
    private String category;

    @Size(max = 200, message = "規格說明不可超過 200 字元")
    private String specification;

    @DecimalMin(value = "0.00", inclusive = true, message = "安全庫存量不可為負數")
    private BigDecimal safeStockQuantity;

    @DecimalMin(value = "0.00", inclusive = true, message = "目前庫存量不可為負數")
    private BigDecimal currentStockQuantity;

    @DecimalMin(value = "0.00", inclusive = true, message = "成本單價不可為負數")
    private BigDecimal costPrice;

    @Size(max = 500, message = "備註不可超過 500 字元")
    private String note;
}
