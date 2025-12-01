package com.morningharvest.erp.payment.dto;

import com.morningharvest.erp.payment.entity.PaymentTransaction;
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
public class PaymentTransactionDTO {

    private Long id;
    private Long orderId;
    private String paymentMethod;
    private String status;
    private BigDecimal amount;
    private BigDecimal amountReceived;
    private BigDecimal changeAmount;
    private String referenceNo;
    private String note;
    private LocalDateTime transactionTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentTransactionDTO from(PaymentTransaction transaction) {
        return PaymentTransactionDTO.builder()
                .id(transaction.getId())
                .orderId(transaction.getOrderId())
                .paymentMethod(transaction.getPaymentMethod())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .amountReceived(transaction.getAmountReceived())
                .changeAmount(transaction.getChangeAmount())
                .referenceNo(transaction.getReferenceNo())
                .note(transaction.getNote())
                .transactionTime(transaction.getTransactionTime())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
