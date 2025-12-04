package com.morningharvest.erp.invoice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "payment_transaction_id")
    private Long paymentTransactionId;

    // 發票識別
    @Column(name = "invoice_number", length = 20)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "invoice_period", length = 5)
    private String invoicePeriod;

    // 發票類型
    @Column(name = "invoice_type", length = 20)
    @Builder.Default
    private String invoiceType = "B2C";

    @Column(name = "issue_type", length = 20)
    @Builder.Default
    private String issueType = "ELECTRONIC";

    // 買方資訊
    @Column(name = "buyer_identifier", length = 10)
    private String buyerIdentifier;

    @Column(name = "buyer_name", length = 100)
    private String buyerName;

    // 載具資訊 (B2C)
    @Column(name = "carrier_type", length = 20)
    private String carrierType;

    @Column(name = "carrier_value", length = 64)
    private String carrierValue;

    // 捐贈 (B2C)
    @Column(name = "is_donated")
    @Builder.Default
    private Boolean isDonated = false;

    @Column(name = "donate_code", length = 10)
    private String donateCode;

    // 金額資訊
    @Column(name = "sales_amount", precision = 10, scale = 2)
    private BigDecimal salesAmount;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // 狀態追蹤
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ISSUED";

    @Column(name = "external_invoice_id", length = 50)
    private String externalInvoiceId;

    @Column(name = "issue_result_code", length = 20)
    private String issueResultCode;

    @Column(name = "issue_result_message", length = 500)
    private String issueResultMessage;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    // 列印追蹤
    @Column(name = "is_printed")
    @Builder.Default
    private Boolean isPrinted = false;

    @Column(name = "print_count")
    @Builder.Default
    private Integer printCount = 0;

    @Column(name = "last_printed_at")
    private LocalDateTime lastPrintedAt;

    // 作廢相關
    @Column(name = "is_voided")
    @Builder.Default
    private Boolean isVoided = false;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "void_reason", length = 200)
    private String voidReason;

    // 時間戳記
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
