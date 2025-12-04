package com.morningharvest.erp.invoice.dto;

import com.morningharvest.erp.invoice.entity.InvoiceAllowance;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 發票折讓 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "發票折讓資訊")
public class InvoiceAllowanceDTO {

    @Schema(description = "折讓記錄 ID", example = "1")
    private Long id;

    @Schema(description = "原發票 ID", example = "789")
    private Long invoiceId;

    @Schema(description = "折讓單號碼", example = "AA-20000001")
    private String allowanceNumber;

    @Schema(description = "折讓日期", example = "2024-12-03")
    private LocalDate allowanceDate;

    @Schema(description = "折讓銷售額 (未稅)", example = "48.00")
    private BigDecimal salesAmount;

    @Schema(description = "折讓稅額", example = "2.00")
    private BigDecimal taxAmount;

    @Schema(description = "折讓總額 (含稅)", example = "50.00")
    private BigDecimal totalAmount;

    @Schema(description = "狀態: ISSUED / FAILED", example = "ISSUED")
    private String status;

    @Schema(description = "折讓原因", example = "商品瑕疵退款")
    private String reason;

    @Schema(description = "建立時間")
    private LocalDateTime createdAt;

    public static InvoiceAllowanceDTO from(InvoiceAllowance entity) {
        return InvoiceAllowanceDTO.builder()
                .id(entity.getId())
                .invoiceId(entity.getInvoiceId())
                .allowanceNumber(entity.getAllowanceNumber())
                .allowanceDate(entity.getAllowanceDate())
                .salesAmount(entity.getSalesAmount())
                .taxAmount(entity.getTaxAmount())
                .totalAmount(entity.getTotalAmount())
                .status(entity.getStatus())
                .reason(entity.getReason())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
