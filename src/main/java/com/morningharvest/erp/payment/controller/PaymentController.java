package com.morningharvest.erp.payment.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.payment.dto.PaymentTransactionDTO;
import com.morningharvest.erp.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "付款管理", description = "付款交易記錄管理")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/list")
    @Operation(summary = "查詢付款記錄", description = "根據訂單 ID 查詢付款交易記錄")
    public ApiResponse<List<PaymentTransactionDTO>> getPaymentsByOrderId(
            @Parameter(description = "訂單 ID", required = true, example = "1")
            @RequestParam("orderId") Long orderId
    ) {
        log.debug("查詢付款記錄, orderId: {}", orderId);
        List<PaymentTransactionDTO> payments = paymentService.getPaymentsByOrderId(orderId);
        return ApiResponse.success(payments);
    }
}
