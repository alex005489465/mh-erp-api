package com.morningharvest.erp.material.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialDTO {

    private Long id;
    private String code;
    private String name;
    private String unit;
    private String unitDisplayName;
    private String category;
    private String categoryDisplayName;
    private String specification;
    private BigDecimal safeStockQuantity;
    private BigDecimal currentStockQuantity;
    private BigDecimal costPrice;
    private Boolean isActive;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
