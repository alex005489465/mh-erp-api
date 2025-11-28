package com.morningharvest.erp.combo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchCreateComboItemRequest {

    @NotNull(message = "套餐 ID 不可為空")
    private Long comboId;

    @NotEmpty(message = "項目清單不可為空")
    @Valid
    private List<ComboItemInput> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComboItemInput {

        @NotNull(message = "商品 ID 不可為空")
        private Long productId;

        private Integer quantity;

        private Integer sortOrder;
    }
}
