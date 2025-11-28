package com.morningharvest.erp.combo.dto;

import com.morningharvest.erp.product.dto.ProductOptionGroupDetailDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComboItemDetailDTO {

    private Long id;
    private Long comboId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductOptionGroupDetailDTO> optionGroups;
}
