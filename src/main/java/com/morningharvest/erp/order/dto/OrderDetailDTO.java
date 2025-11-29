package com.morningharvest.erp.order.dto;

import com.morningharvest.erp.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailDTO {

    private Long id;
    private String status;
    private String orderType;
    private BigDecimal totalAmount;
    private String note;
    private List<OrderItemDTO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderDetailDTO from(Order order, List<OrderItemDTO> items) {
        return OrderDetailDTO.builder()
                .id(order.getId())
                .status(order.getStatus())
                .orderType(order.getOrderType())
                .totalAmount(order.getTotalAmount())
                .note(order.getNote())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
