package com.morningharvest.erp.invoice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 對外部發票服務的作廢請求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoidInvoiceExternalRequest {

    /**
     * ERP 發票記錄 ID (用於冪等性)
     */
    private String requestId;

    /**
     * 發票號碼
     */
    private String invoiceNumber;

    /**
     * 作廢原因
     */
    private String reason;
}
