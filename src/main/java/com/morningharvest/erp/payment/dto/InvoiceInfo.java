package com.morningharvest.erp.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 結帳時的發票資訊
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "發票資訊 (結帳時傳入)")
public class InvoiceInfo {

    @Schema(description = "發票類型: B2C / B2B", example = "B2C", defaultValue = "B2C")
    @Builder.Default
    private String invoiceType = "B2C";

    @Schema(description = "開立類型: ELECTRONIC (電子) / PAPER (紙本)", example = "ELECTRONIC", defaultValue = "ELECTRONIC")
    @Builder.Default
    private String issueType = "ELECTRONIC";

    @Schema(description = "買方統編 (B2B 時必填)", example = "12345678")
    private String buyerIdentifier;

    @Schema(description = "買方名稱 (B2B 時填寫)", example = "範例公司")
    private String buyerName;

    @Schema(description = "載具類型: MOBILE_BARCODE / NATURAL_PERSON / MEMBER / NONE", example = "MOBILE_BARCODE")
    private String carrierType;

    @Schema(description = "載具號碼", example = "/ABC1234")
    private String carrierValue;

    @Schema(description = "是否捐贈", example = "false", defaultValue = "false")
    @Builder.Default
    private Boolean isDonated = false;

    @Schema(description = "愛心碼 (捐贈對象代碼)", example = "168001")
    private String donateCode;
}
