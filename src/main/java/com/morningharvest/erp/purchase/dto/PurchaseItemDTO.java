package com.morningharvest.erp.purchase.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseItemDTO {

    private Long id;
    private Long purchaseId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String materialUnit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
