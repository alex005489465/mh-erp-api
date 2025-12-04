package com.morningharvest.erp.invoice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 外部發票服務的折讓回應
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllowanceExternalResponse {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 折讓單號碼
     */
    private String allowanceNumber;

    /**
     * 折讓日期
     */
    private LocalDate allowanceDate;

    /**
     * 外部折讓 ID
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

    public static AllowanceExternalResponse success(String allowanceNumber) {
        return AllowanceExternalResponse.builder()
                .success(true)
                .allowanceNumber(allowanceNumber)
                .allowanceDate(LocalDate.now())
                .resultCode("SUCCESS")
                .resultMessage("折讓開立成功")
                .build();
    }

    public static AllowanceExternalResponse failure(String code, String message) {
        return AllowanceExternalResponse.builder()
                .success(false)
                .resultCode(code)
                .resultMessage(message)
                .build();
    }
}
