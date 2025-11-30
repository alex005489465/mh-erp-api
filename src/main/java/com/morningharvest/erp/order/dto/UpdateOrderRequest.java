package com.morningharvest.erp.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 更新訂單請求 DTO
 * 整批取代訂單項目
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderRequest {

    @Pattern(regexp = "^(DINE_IN|TAKEOUT|DELIVERY)$", message = "訂單類型必須為 DINE_IN、TAKEOUT 或 DELIVERY")
    private String orderType;

    private String note;

    /**
     * 訂單項目列表（將取代所有現有項目）
     */
    @Valid
    @NotNull(message = "訂單項目不能為空")
    private List<OrderItemRequest> items;
}
