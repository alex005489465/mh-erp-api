package com.morningharvest.erp.payment.dto;

import com.morningharvest.erp.invoice.dto.InvoiceResult;
import com.morningharvest.erp.order.dto.OrderDTO;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "結帳回應")
public class CheckoutResponse {

    @Schema(description = "付款交易 ID", example = "456")
    private Long transactionId;

    @Schema(description = "訂單 ID", example = "123")
    private Long orderId;

    @Schema(description = "付款方式", example = "CASH")
    private String paymentMethod;

    @Schema(description = "付款狀態", example = "COMPLETED")
    private String status;

    @Schema(description = "應付金額", example = "100.00")
    private BigDecimal amount;

    @Schema(description = "實收金額", example = "100.00")
    private BigDecimal amountReceived;

    @Schema(description = "找零金額", example = "0.00")
    private BigDecimal changeAmount;

    @Schema(description = "交易時間")
    private LocalDateTime transactionTime;

    @Schema(description = "訂單資訊")
    private OrderDTO order;

    @Schema(description = "發票開立結果")
    private InvoiceResult invoice;
}
