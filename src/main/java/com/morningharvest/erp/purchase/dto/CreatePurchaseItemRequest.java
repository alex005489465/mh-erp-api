package com.morningharvest.erp.purchase.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePurchaseItemRequest {

    @NotNull(message = "原物料ID不可為空")
    private Long materialId;

    @NotNull(message = "數量不可為空")
    @DecimalMin(value = "0.01", message = "數量必須大於 0")
    private BigDecimal quantity;

    @NotNull(message = "單價不可為空")
    @DecimalMin(value = "0", message = "單價不可為負數")
    private BigDecimal unitPrice;

    @Size(max = 200, message = "備註不可超過 200 字元")
    private String note;
}
