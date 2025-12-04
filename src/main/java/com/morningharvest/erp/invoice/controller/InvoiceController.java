package com.morningharvest.erp.invoice.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.invoice.dto.*;
import com.morningharvest.erp.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "發票管理", description = "發票開立、作廢、折讓等操作")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/issue")
    @Operation(summary = "開立發票", description = "為已付款的訂單開立發票")
    public ApiResponse<InvoiceResult> issueInvoice(@RequestBody IssueInvoiceRequest request) {
        log.info("開立發票請求, orderId: {}", request.getOrderId());
        InvoiceResult result = invoiceService.issueInvoice(request);
        return ApiResponse.success(result);
    }

    @PostMapping("/void")
    @Operation(summary = "作廢發票", description = "作廢當月已開立的發票")
    public ApiResponse<InvoiceResult> voidInvoice(@RequestBody VoidInvoiceRequest request) {
        log.info("作廢發票請求, invoiceId: {}", request.getInvoiceId());
        InvoiceResult result = invoiceService.voidInvoice(request);
        return ApiResponse.success(result);
    }

    @PostMapping("/allowance")
    @Operation(summary = "開立折讓", description = "為已開立的發票開立折讓 (跨月退款用)")
    public ApiResponse<InvoiceAllowanceDTO> createAllowance(@RequestBody CreateAllowanceRequest request) {
        log.info("開立折讓請求, invoiceId: {}, amount: {}", request.getInvoiceId(), request.getAmount());
        InvoiceAllowanceDTO result = invoiceService.createAllowance(request);
        return ApiResponse.success(result);
    }

    @PostMapping("/print")
    @Operation(summary = "記錄列印", description = "記錄發票已列印，更新列印次數")
    public ApiResponse<InvoiceDTO> recordPrint(
            @Parameter(description = "發票 ID", required = true, example = "789")
            @RequestParam("id") Long invoiceId
    ) {
        log.info("記錄發票列印, invoiceId: {}", invoiceId);
        InvoiceDTO result = invoiceService.recordPrint(invoiceId);
        return ApiResponse.success(result);
    }

    @GetMapping("/detail")
    @Operation(summary = "查詢發票詳情", description = "根據發票 ID 查詢發票詳情")
    public ApiResponse<InvoiceDTO> getInvoiceById(
            @Parameter(description = "發票 ID", required = true, example = "789")
            @RequestParam("id") Long invoiceId
    ) {
        log.debug("查詢發票詳情, invoiceId: {}", invoiceId);
        InvoiceDTO result = invoiceService.getInvoiceById(invoiceId);
        return ApiResponse.success(result);
    }

    @GetMapping("/by-order")
    @Operation(summary = "依訂單查詢發票", description = "根據訂單 ID 查詢發票")
    public ApiResponse<InvoiceDTO> getInvoiceByOrderId(
            @Parameter(description = "訂單 ID", required = true, example = "123")
            @RequestParam("orderId") Long orderId
    ) {
        log.debug("依訂單查詢發票, orderId: {}", orderId);
        InvoiceDTO result = invoiceService.getInvoiceByOrderId(orderId);
        return ApiResponse.success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "查詢發票列表", description = "分頁查詢發票列表，可依日期範圍和狀態篩選")
    public ApiResponse<PageResponse<InvoiceDTO>> listInvoices(
            @Parameter(description = "開始日期 (YYYY-MM-DD)", example = "2024-01-01")
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "結束日期 (YYYY-MM-DD)", example = "2024-01-31")
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "狀態: ISSUED / VOID / FAILED", example = "ISSUED")
            @RequestParam(value = "status", required = false) String status,

            @Parameter(description = "頁碼 (從 1 開始)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,

            @Parameter(description = "每頁筆數", example = "20")
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        log.debug("查詢發票列表, startDate: {}, endDate: {}, status: {}, page: {}, size: {}",
                startDate, endDate, status, page, size);

        // 將 1-based 頁碼轉換為 0-based
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<InvoiceDTO> invoices = invoiceService.listInvoices(startDate, endDate, status, pageable);

        return ApiResponse.success(PageResponse.from(invoices));
    }

    @GetMapping("/allowances")
    @Operation(summary = "查詢發票的折讓記錄", description = "根據發票 ID 查詢所有折讓記錄")
    public ApiResponse<List<InvoiceAllowanceDTO>> getAllowancesByInvoiceId(
            @Parameter(description = "發票 ID", required = true, example = "789")
            @RequestParam("invoiceId") Long invoiceId
    ) {
        log.debug("查詢發票折讓記錄, invoiceId: {}", invoiceId);
        List<InvoiceAllowanceDTO> allowances = invoiceService.getAllowancesByInvoiceId(invoiceId);
        return ApiResponse.success(allowances);
    }
}
