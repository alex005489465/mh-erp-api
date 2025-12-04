package com.morningharvest.erp.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作廢發票請求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "作廢發票請求")
public class VoidInvoiceRequest {

    @Schema(description = "發票 ID", example = "789", required = true)
    private Long invoiceId;

    @Schema(description = "作廢原因", example = "客戶要求取消", required = true)
    private String reason;
}
