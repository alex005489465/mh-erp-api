package com.morningharvest.erp.invoice.dto;

import com.morningharvest.erp.invoice.entity.InvoiceItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 發票明細 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "發票明細")
public class InvoiceItemDTO {

    @Schema(description = "明細 ID", example = "1")
    private Long id;

    @Schema(description = "項次序號", example = "1")
    private Integer sequence;

    @Schema(description = "品名", example = "招牌蛋餅")
    private String description;

    @Schema(description = "數量", example = "1")
    private BigDecimal quantity;

    @Schema(description = "單價", example = "45.00")
    private BigDecimal unitPrice;

    @Schema(description = "金額", example = "45.00")
    private BigDecimal amount;

    @Schema(description = "備註", example = "加辣")
    private String note;

    public static InvoiceItemDTO from(InvoiceItem entity) {
        return InvoiceItemDTO.builder()
                .id(entity.getId())
                .sequence(entity.getSequence())
                .description(entity.getDescription())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .amount(entity.getAmount())
                .note(entity.getNote())
                .build();
    }
}
