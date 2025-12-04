package com.morningharvest.erp.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "結帳請求")
public class CheckoutRequest {

    @NotNull(message = "訂單 ID 不可為空")
    @Schema(description = "訂單 ID", example = "123", required = true)
    private Long orderId;

    @NotBlank(message = "付款方式不可為空")
    @Builder.Default
    @Schema(description = "付款方式", example = "CASH", defaultValue = "CASH")
    private String paymentMethod = "CASH";

    @NotNull(message = "實收金額不可為空")
    @DecimalMin(value = "0.01", message = "實收金額必須大於零")
    @Schema(description = "實收金額", example = "100.00", required = true)
    private BigDecimal amountReceived;

    @NotNull(message = "找零金額不可為空")
    @DecimalMin(value = "0.00", message = "找零金額不可為負數")
    @Schema(description = "找零金額", example = "0.00", required = true)
    private BigDecimal changeAmount;

    @Schema(description = "備註", example = "現金付款")
    private String note;

    @Schema(description = "發票資訊 (付款完成後自動開立發票)")
    private InvoiceInfo invoice;
}
