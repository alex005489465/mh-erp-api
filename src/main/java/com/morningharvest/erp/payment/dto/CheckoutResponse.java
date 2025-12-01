package com.morningharvest.erp.payment.dto;

import com.morningharvest.erp.order.dto.OrderDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutResponse {

    private Long transactionId;
    private Long orderId;
    private String paymentMethod;
    private String status;
    private BigDecimal amount;
    private LocalDateTime transactionTime;
    private OrderDTO order;
}
