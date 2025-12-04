package com.morningharvest.erp.invoice.client;

import com.morningharvest.erp.invoice.client.dto.*;

/**
 * 發票服務客戶端介面
 * 用於與外部發票服務溝通 (財政部/加值中心)
 */
public interface InvoiceServiceClient {

    /**
     * 開立發票
     */
    IssueInvoiceExternalResponse issueInvoice(IssueInvoiceExternalRequest request);

    /**
     * 作廢發票
     */
    VoidInvoiceExternalResponse voidInvoice(VoidInvoiceExternalRequest request);

    /**
     * 開立折讓
     */
    AllowanceExternalResponse createAllowance(AllowanceExternalRequest request);
}
