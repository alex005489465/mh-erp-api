package com.morningharvest.erp.inventorycheck.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInventoryCheckItemRequest {

    @NotNull(message = "明細 ID 不可為空")
    private Long itemId;

    @NotNull(message = "實際盤點數量不可為空")
    @DecimalMin(value = "0", message = "實際盤點數量必須 >= 0")
    private BigDecimal actualQuantity;

    @Size(max = 200, message = "備註不可超過 200 字元")
    private String note;
}
