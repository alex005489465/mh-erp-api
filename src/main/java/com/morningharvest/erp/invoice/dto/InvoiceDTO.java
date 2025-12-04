package com.morningharvest.erp.invoice.dto;

import com.morningharvest.erp.invoice.entity.Invoice;
import com.morningharvest.erp.invoice.entity.InvoiceItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 發票 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "發票資訊")
public class InvoiceDTO {

    @Schema(description = "發票記錄 ID", example = "789")
    private Long id;

    @Schema(description = "關聯訂單 ID", example = "123")
    private Long orderId;

    @Schema(description = "關聯付款交易 ID", example = "456")
    private Long paymentTransactionId;

    // 發票識別
    @Schema(description = "發票號碼", example = "AB-12345678")
    private String invoiceNumber;

    @Schema(description = "發票日期", example = "2024-12-03")
    private LocalDate invoiceDate;

    @Schema(description = "發票期別", example = "11312")
    private String invoicePeriod;

    // 發票類型
    @Schema(description = "發票類型: B2C / B2B", example = "B2C")
    private String invoiceType;

    @Schema(description = "開立類型: ELECTRONIC / PAPER", example = "ELECTRONIC")
    private String issueType;

    // 買方資訊
    @Schema(description = "買方統編", example = "12345678")
    private String buyerIdentifier;

    @Schema(description = "買方名稱", example = "範例公司")
    private String buyerName;

    // 載具資訊
    @Schema(description = "載具類型", example = "MOBILE_BARCODE")
    private String carrierType;

    @Schema(description = "載具號碼", example = "/ABC1234")
    private String carrierValue;

    // 捐贈
    @Schema(description = "是否捐贈", example = "false")
    private Boolean isDonated;

    @Schema(description = "愛心碼", example = "168001")
    private String donateCode;

    // 金額資訊
    @Schema(description = "銷售額 (未稅)", example = "95.00")
    private BigDecimal salesAmount;

    @Schema(description = "稅額", example = "5.00")
    private BigDecimal taxAmount;

    @Schema(description = "總金額 (含稅)", example = "100.00")
    private BigDecimal totalAmount;

    // 狀態
    @Schema(description = "狀態: ISSUED / VOID / FAILED", example = "ISSUED")
    private String status;

    @Schema(description = "開立成功時間")
    private LocalDateTime issuedAt;

    // 列印追蹤
    @Schema(description = "是否已列印", example = "false")
    private Boolean isPrinted;

    @Schema(description = "列印次數", example = "0")
    private Integer printCount;

    @Schema(description = "最後列印時間")
    private LocalDateTime lastPrintedAt;

    // 作廢相關
    @Schema(description = "是否已作廢", example = "false")
    private Boolean isVoided;

    @Schema(description = "作廢時間")
    private LocalDateTime voidedAt;

    @Schema(description = "作廢原因", example = "客戶要求取消")
    private String voidReason;

    // 明細
    @Schema(description = "發票明細")
    private List<InvoiceItemDTO> items;

    // 時間戳記
    @Schema(description = "建立時間")
    private LocalDateTime createdAt;

    @Schema(description = "更新時間")
    private LocalDateTime updatedAt;

    public static InvoiceDTO from(Invoice entity, List<InvoiceItem> items) {
        return InvoiceDTO.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .paymentTransactionId(entity.getPaymentTransactionId())
                .invoiceNumber(entity.getInvoiceNumber())
                .invoiceDate(entity.getInvoiceDate())
                .invoicePeriod(entity.getInvoicePeriod())
                .invoiceType(entity.getInvoiceType())
                .issueType(entity.getIssueType())
                .buyerIdentifier(entity.getBuyerIdentifier())
                .buyerName(entity.getBuyerName())
                .carrierType(entity.getCarrierType())
                .carrierValue(entity.getCarrierValue())
                .isDonated(entity.getIsDonated())
                .donateCode(entity.getDonateCode())
                .salesAmount(entity.getSalesAmount())
                .taxAmount(entity.getTaxAmount())
                .totalAmount(entity.getTotalAmount())
                .status(entity.getStatus())
                .issuedAt(entity.getIssuedAt())
                .isPrinted(entity.getIsPrinted())
                .printCount(entity.getPrintCount())
                .lastPrintedAt(entity.getLastPrintedAt())
                .isVoided(entity.getIsVoided())
                .voidedAt(entity.getVoidedAt())
                .voidReason(entity.getVoidReason())
                .items(items.stream().map(InvoiceItemDTO::from).toList())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
