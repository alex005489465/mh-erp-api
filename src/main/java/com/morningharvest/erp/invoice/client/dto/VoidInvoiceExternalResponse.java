package com.morningharvest.erp.invoice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 外部發票服務的作廢回應
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoidInvoiceExternalResponse {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 結果代碼
     */
    private String resultCode;

    /**
     * 結果訊息
     */
    private String resultMessage;

    public static VoidInvoiceExternalResponse success() {
        return VoidInvoiceExternalResponse.builder()
                .success(true)
                .resultCode("SUCCESS")
                .resultMessage("作廢成功")
                .build();
    }

    public static VoidInvoiceExternalResponse failure(String code, String message) {
        return VoidInvoiceExternalResponse.builder()
                .success(false)
                .resultCode(code)
                .resultMessage(message)
                .build();
    }
}
