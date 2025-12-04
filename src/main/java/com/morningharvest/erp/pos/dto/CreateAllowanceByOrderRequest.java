package com.morningharvest.erp.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * POS 開立折讓請求 (依訂單 ID)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "POS 開立折讓請求")
public class CreateAllowanceByOrderRequest {

    @NotNull(message = "訂單 ID 不可為空")
    @Schema(description = "訂單 ID", example = "123", required = true)
    private Long orderId;

    @NotNull(message = "折讓金額不可為空")
    @DecimalMin(value = "0.01", message = "折讓金額必須大於零")
    @Schema(description = "折讓金額 (含稅)", example = "50.00", required = true)
    private BigDecimal amount;

    @NotBlank(message = "折讓原因不可為空")
    @Schema(description = "折讓原因", example = "商品瑕疵退款", required = true)
    private String reason;
}
