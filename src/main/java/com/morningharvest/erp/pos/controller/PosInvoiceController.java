package com.morningharvest.erp.pos.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.invoice.dto.InvoiceAllowanceDTO;
import com.morningharvest.erp.invoice.dto.InvoiceDTO;
import com.morningharvest.erp.invoice.dto.InvoiceResult;
import com.morningharvest.erp.invoice.service.InvoiceService;
import com.morningharvest.erp.pos.dto.CreateAllowanceByOrderRequest;
import com.morningharvest.erp.pos.dto.VoidInvoiceByOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pos/invoices")
@RequiredArgsConstructor
@Tag(name = "POS 發票", description = "POS 發票操作")
public class PosInvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/detail")
    @Operation(summary = "查詢發票", description = "依訂單 ID 查詢發票詳情，用於列印收據")
    public ApiResponse<InvoiceDTO> getInvoiceByOrderId(
            @Parameter(description = "訂單 ID", required = true, example = "123")
            @RequestParam("orderId") Long orderId
    ) {
        log.debug("POS 查詢發票, orderId: {}", orderId);
        InvoiceDTO invoice = invoiceService.getInvoiceByOrderId(orderId);
        return ApiResponse.success(invoice);
    }

    @PostMapping("/print")
    @Operation(summary = "記錄列印", description = "記錄發票已列印，更新列印次數")
    public ApiResponse<InvoiceDTO> recordPrint(
            @Parameter(description = "訂單 ID", required = true, example = "123")
            @RequestParam("orderId") Long orderId
    ) {
        log.info("POS 記錄發票列印, orderId: {}", orderId);
        InvoiceDTO invoice = invoiceService.recordPrintByOrderId(orderId);
        return ApiResponse.success(invoice);
    }

    @PostMapping("/void")
    @Operation(summary = "作廢發票", description = "作廢當月已開立的發票。跨月發票請使用折讓")
    public ApiResponse<InvoiceResult> voidInvoice(
            @Valid @RequestBody VoidInvoiceByOrderRequest request
    ) {
        log.info("POS 作廢發票, orderId: {}, reason: {}", request.getOrderId(), request.getReason());
        InvoiceResult result = invoiceService.voidInvoiceByOrderId(request.getOrderId(), request.getReason());
        return ApiResponse.success(result);
    }

    @PostMapping("/allowance")
    @Operation(summary = "開立折讓", description = "為已開立的發票開立折讓，用於跨月退款或部分退款")
    public ApiResponse<InvoiceAllowanceDTO> createAllowance(
            @Valid @RequestBody CreateAllowanceByOrderRequest request
    ) {
        log.info("POS 開立折讓, orderId: {}, amount: {}, reason: {}",
                request.getOrderId(), request.getAmount(), request.getReason());
        InvoiceAllowanceDTO allowance = invoiceService.createAllowanceByOrderId(
                request.getOrderId(), request.getAmount(), request.getReason());
        return ApiResponse.success(allowance);
    }
}
