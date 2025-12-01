package com.morningharvest.erp.pos.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.payment.dto.CheckoutRequest;
import com.morningharvest.erp.payment.dto.CheckoutResponse;
import com.morningharvest.erp.payment.dto.PaymentTransactionDTO;
import com.morningharvest.erp.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pos/orders")
@RequiredArgsConstructor
@Tag(name = "POS 結帳", description = "POS 收銀結帳操作")
public class PosCheckoutController {

    private final PaymentService paymentService;

    @PostMapping("/checkout")
    @Operation(summary = "結帳付款", description = "對 POS 訂單進行結帳付款")
    public ApiResponse<CheckoutResponse> checkout(
            @Valid @RequestBody CheckoutRequest request
    ) {
        log.info("POS 結帳, orderId: {}", request.getOrderId());
        CheckoutResponse response = paymentService.checkout(request);
        return ApiResponse.success("付款成功", response);
    }

    @GetMapping("/payment")
    @Operation(summary = "查詢付款資訊", description = "查詢訂單的付款資訊（單筆），用於顯示待付款或已付款資訊")
    public ApiResponse<PaymentTransactionDTO> getPayment(
            @Parameter(description = "訂單 ID", required = true, example = "1")
            @RequestParam("orderId") Long orderId
    ) {
        log.debug("POS 查詢付款資訊, orderId: {}", orderId);
        PaymentTransactionDTO payment = paymentService.getPaymentByOrderId(orderId);
        return ApiResponse.success(payment);
    }

    @GetMapping("/payments")
    @Operation(summary = "查詢付款記錄列表", description = "查詢訂單的所有付款交易記錄")
    public ApiResponse<List<PaymentTransactionDTO>> getPayments(
            @Parameter(description = "訂單 ID", required = true, example = "1")
            @RequestParam("orderId") Long orderId
    ) {
        log.debug("POS 查詢付款記錄, orderId: {}", orderId);
        List<PaymentTransactionDTO> payments = paymentService.getPaymentsByOrderId(orderId);
        return ApiResponse.success(payments);
    }
}
