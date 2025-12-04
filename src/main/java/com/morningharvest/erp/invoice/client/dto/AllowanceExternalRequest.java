package com.morningharvest.erp.invoice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 對外部發票服務的折讓請求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllowanceExternalRequest {

    /**
     * ERP 折讓記錄 ID (用於冪等性)
     */
    private String requestId;

    /**
     * 原發票號碼
     */
    private String invoiceNumber;

    /**
     * 折讓銷售額 (未稅)
     */
    private BigDecimal salesAmount;

    /**
     * 折讓稅額
     */
    private BigDecimal taxAmount;

    /**
     * 折讓總額 (含稅)
     */
    private BigDecimal totalAmount;

    /**
     * 折讓原因
     */
    private String reason;
}
