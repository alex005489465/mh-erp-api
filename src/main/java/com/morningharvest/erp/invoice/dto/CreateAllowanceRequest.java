package com.morningharvest.erp.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 開立折讓請求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "開立折讓請求")
public class CreateAllowanceRequest {

    @Schema(description = "發票 ID", example = "789", required = true)
    private Long invoiceId;

    @Schema(description = "折讓金額 (含稅)", example = "50.00", required = true)
    private BigDecimal amount;

    @Schema(description = "折讓原因", example = "商品瑕疵退款", required = true)
    private String reason;
}
