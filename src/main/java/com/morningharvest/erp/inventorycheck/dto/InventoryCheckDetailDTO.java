package com.morningharvest.erp.inventorycheck.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckDetailDTO {

    private Long id;
    private String checkNumber;
    private String status;
    private String statusDisplayName;
    private LocalDate checkDate;
    private Integer totalItems;
    private Integer checkedItems;
    private Integer uncheckedItems;
    private BigDecimal totalDifferenceAmount;
    private String note;
    private LocalDateTime startedAt;
    private String startedBy;
    private LocalDateTime confirmedAt;
    private String confirmedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<InventoryCheckItemDTO> items;
}
