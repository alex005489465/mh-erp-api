package com.morningharvest.erp.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POS 作廢發票請求 (依訂單 ID)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "POS 作廢發票請求")
public class VoidInvoiceByOrderRequest {

    @NotNull(message = "訂單 ID 不可為空")
    @Schema(description = "訂單 ID", example = "123", required = true)
    private Long orderId;

    @NotBlank(message = "作廢原因不可為空")
    @Schema(description = "作廢原因", example = "客戶要求取消", required = true)
    private String reason;
}
