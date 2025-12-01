package com.morningharvest.erp.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {

    @NotNull(message = "訂單 ID 不可為空")
    private Long orderId;

    @NotBlank(message = "付款方式不可為空")
    @Builder.Default
    private String paymentMethod = "CASH";

    @NotNull(message = "付款金額不可為空")
    @DecimalMin(value = "0.01", message = "付款金額必須大於零")
    private BigDecimal amount;

    private String note;
}
