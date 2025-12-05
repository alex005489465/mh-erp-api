package com.morningharvest.erp.purchase.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseDTO {

    private Long id;
    private String purchaseNumber;
    private Long supplierId;
    private String supplierName;
    private String status;
    private String statusDisplayName;
    private BigDecimal totalAmount;
    private LocalDate purchaseDate;
    private String note;
    private LocalDateTime confirmedAt;
    private String confirmedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
