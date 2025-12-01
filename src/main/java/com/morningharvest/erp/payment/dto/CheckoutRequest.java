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

    @NotNull(message = "實收金額不可為空")
    @DecimalMin(value = "0.01", message = "實收金額必須大於零")
    private BigDecimal amountReceived;

    @NotNull(message = "找零金額不可為空")
    @DecimalMin(value = "0.00", message = "找零金額不可為負數")
    private BigDecimal changeAmount;

    private String note;
}
