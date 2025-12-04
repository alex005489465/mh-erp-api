package com.morningharvest.erp.table.dto;

import com.morningharvest.erp.table.entity.DiningTable;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TableDTO {

    private Long id;
    private String tableNumber;
    private Integer capacity;
    private String status;
    private Long currentOrderId;
    private Boolean isActive;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TableDTO from(DiningTable table) {
        return TableDTO.builder()
                .id(table.getId())
                .tableNumber(table.getTableNumber())
                .capacity(table.getCapacity())
                .status(table.getStatus())
                .currentOrderId(table.getCurrentOrderId())
                .isActive(table.getIsActive())
                .note(table.getNote())
                .createdAt(table.getCreatedAt())
                .updatedAt(table.getUpdatedAt())
                .build();
    }
}
