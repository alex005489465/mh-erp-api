package com.morningharvest.erp.inventorycheck.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckItemDTO {

    private Long id;
    private Long inventoryCheckId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String materialUnit;
    private BigDecimal systemQuantity;
    private BigDecimal actualQuantity;
    private BigDecimal differenceQuantity;
    private BigDecimal unitCost;
    private BigDecimal differenceAmount;
    private Boolean isChecked;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
