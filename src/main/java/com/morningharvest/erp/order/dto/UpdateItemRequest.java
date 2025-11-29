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
public class UpdateItemRequest {

    @NotNull(message = "項目 ID 不可為空")
    private Long itemId;

    @Min(value = 1, message = "數量至少為 1")
    private Integer quantity;

    private List<OrderItemOptionDTO> options;

    private String note;
}
