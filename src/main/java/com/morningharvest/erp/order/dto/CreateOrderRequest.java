package com.morningharvest.erp.order.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @Pattern(regexp = "^(DINE_IN|TAKEOUT|DELIVERY)$", message = "訂單類型必須為 DINE_IN、TAKEOUT 或 DELIVERY")
    private String orderType;

    private String note;
}
