package com.morningharvest.erp.pos.dto;

import com.morningharvest.erp.product.dto.ProductOptionGroupDetailDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 套餐內商品資訊 DTO
 * 供前端顯示套餐內各商品及其選項
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComboItemInfoDTO {

    private Long productId;
    private String productName;
    private Integer quantity;
    private Integer sortOrder;

    /**
     * 該商品的選項群組（供客製化用）
     */
    private List<ProductOptionGroupDetailDTO> optionGroups;
}
