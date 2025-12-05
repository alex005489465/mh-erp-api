package com.morningharvest.erp.product.dto;

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
public class ProductRecipeDTO {

    private Long id;
    private Long productId;
    private String productName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal quantity;
    private String unit;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
