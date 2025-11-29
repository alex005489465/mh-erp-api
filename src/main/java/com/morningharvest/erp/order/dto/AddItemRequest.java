package com.morningharvest.erp.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddItemRequest {

    @NotNull(message = "訂單 ID 不可為空")
    private Long orderId;

    @NotNull(message = "商品 ID 不可為空")
    private Long productId;

    @Min(value = 1, message = "數量至少為 1")
    @Builder.Default
    private Integer quantity = 1;

    private List<OrderItemOptionDTO> options;

    private String note;
}
