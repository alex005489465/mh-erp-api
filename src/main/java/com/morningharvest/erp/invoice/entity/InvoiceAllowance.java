package com.morningharvest.erp.invoice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_allowances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceAllowance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "allowance_number", length = 20)
    private String allowanceNumber;

    @Column(name = "allowance_date")
    private LocalDate allowanceDate;

    // 金額
    @Column(name = "sales_amount", precision = 10, scale = 2)
    private BigDecimal salesAmount;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // 狀態
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ISSUED";

    @Column(name = "reason", length = 200)
    private String reason;

    @Column(name = "external_allowance_id", length = 50)
    private String externalAllowanceId;

    @Column(name = "result_code", length = 20)
    private String resultCode;

    @Column(name = "result_message", length = 500)
    private String resultMessage;

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
