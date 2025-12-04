package com.morningharvest.erp.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 開立發票請求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "開立發票請求")
public class IssueInvoiceRequest {

    @Schema(description = "訂單 ID", example = "123", required = true)
    private Long orderId;

    @Schema(description = "付款交易 ID", example = "456", required = true)
    private Long paymentTransactionId;

    @Schema(description = "發票類型: B2C / B2B", example = "B2C", defaultValue = "B2C")
    private String invoiceType;

    @Schema(description = "開立類型: ELECTRONIC (電子) / PAPER (紙本)", example = "ELECTRONIC", defaultValue = "ELECTRONIC")
    private String issueType;

    @Schema(description = "買方統編 (B2B 時必填)", example = "12345678")
    private String buyerIdentifier;

    @Schema(description = "買方名稱 (B2B 時填寫)", example = "範例公司")
    private String buyerName;

    @Schema(description = "載具類型: MOBILE_BARCODE / NATURAL_PERSON / MEMBER / NONE", example = "MOBILE_BARCODE")
    private String carrierType;

    @Schema(description = "載具號碼", example = "/ABC1234")
    private String carrierValue;

    @Schema(description = "是否捐贈", example = "false", defaultValue = "false")
    private Boolean isDonated;

    @Schema(description = "愛心碼 (捐贈對象代碼)", example = "168001")
    private String donateCode;
}
