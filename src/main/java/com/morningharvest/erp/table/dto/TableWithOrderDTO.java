package com.morningharvest.erp.table.dto;

import com.morningharvest.erp.order.dto.OrderDTO;
import com.morningharvest.erp.table.entity.DiningTable;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TableWithOrderDTO {

    private Long id;
    private String tableNumber;
    private Integer capacity;
    private String status;
    private OrderDTO currentOrder;
    private LocalDateTime seatedAt;

    public static TableWithOrderDTO from(DiningTable table, OrderDTO currentOrder) {
        return TableWithOrderDTO.builder()
                .id(table.getId())
                .tableNumber(table.getTableNumber())
                .capacity(table.getCapacity())
                .status(table.getStatus())
                .currentOrder(currentOrder)
                .seatedAt(currentOrder != null ? currentOrder.getCreatedAt() : null)
                .build();
    }
}
