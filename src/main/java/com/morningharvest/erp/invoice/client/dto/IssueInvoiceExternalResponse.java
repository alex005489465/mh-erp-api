package com.morningharvest.erp.invoice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 外部發票服務的開立回應
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueInvoiceExternalResponse {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 發票號碼 (如 AB-12345678)
     */
    private String invoiceNumber;

    /**
     * 發票日期
     */
    private LocalDate invoiceDate;

    /**
     * 發票期別 (如 11312)
     */
    private String invoicePeriod;

    /**
     * 發票服務的外部 ID
     */
    private String externalId;

    /**
     * 結果代碼
     */
    private String resultCode;

    /**
     * 結果訊息
     */
    private String resultMessage;
}
