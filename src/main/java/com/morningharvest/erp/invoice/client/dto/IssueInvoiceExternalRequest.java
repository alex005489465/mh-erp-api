package com.morningharvest.erp.invoice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 對外部發票服務的開立請求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueInvoiceExternalRequest {

    /**
     * ERP 發票記錄 ID (用於冪等性)
     */
    private String requestId;

    /**
     * 發票類型: B2C / B2B
     */
    private String invoiceType;

    /**
     * 開立類型: ELECTRONIC / PAPER
     */
    private String issueType;

    /**
     * 買方資訊
     */
    private Buyer buyer;

    /**
     * 載具資訊 (B2C)
     */
    private Carrier carrier;

    /**
     * 捐贈資訊
     */
    private Donation donation;

    /**
     * 金額資訊
     */
    private Amounts amounts;

    /**
     * 發票明細
     */
    private List<Item> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Buyer {
        private String identifier;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Carrier {
        private String type;
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Donation {
        private Boolean enabled;
        private String code;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Amounts {
        private BigDecimal salesAmount;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
    }
}
