package com.morningharvest.erp.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 發票操作結果 (用於嵌入 CheckoutResponse)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "發票操作結果")
public class InvoiceResult {

    @Schema(description = "ERP 發票記錄 ID", example = "789")
    private Long invoiceId;

    @Schema(description = "發票號碼", example = "AB-12345678")
    private String invoiceNumber;

    @Schema(description = "狀態: ISSUED / FAILED", example = "ISSUED")
    private String status;

    @Schema(description = "結果訊息", example = "開立成功")
    private String message;
}
