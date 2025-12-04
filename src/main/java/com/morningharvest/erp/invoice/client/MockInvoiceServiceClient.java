package com.morningharvest.erp.invoice.client;

import com.morningharvest.erp.invoice.client.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock 發票服務客戶端
 * 在發票服務尚未實作前使用，產生假發票號碼
 */
@Service
@ConditionalOnProperty(name = "invoice.service.mock", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MockInvoiceServiceClient implements InvoiceServiceClient {

    private final AtomicLong invoiceSequence = new AtomicLong(10000001);
    private final AtomicLong allowanceSequence = new AtomicLong(20000001);

    @Override
    public IssueInvoiceExternalResponse issueInvoice(IssueInvoiceExternalRequest request) {
        log.info("[Mock] 開立發票請求: requestId={}, type={}, amount={}",
                request.getRequestId(),
                request.getInvoiceType(),
                request.getAmounts() != null ? request.getAmounts().getTotalAmount() : null);

        // 模擬發票號碼: AA-00000001
        String invoiceNumber = "AA-" + String.format("%08d", invoiceSequence.getAndIncrement());
        String externalId = "mock-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("[Mock] 開立發票成功: invoiceNumber={}", invoiceNumber);

        return IssueInvoiceExternalResponse.builder()
                .success(true)
                .invoiceNumber(invoiceNumber)
                .invoiceDate(LocalDate.now())
                .invoicePeriod(getCurrentPeriod())
                .externalId(externalId)
                .resultCode("SUCCESS")
                .resultMessage("Mock 開立成功")
                .build();
    }

    @Override
    public VoidInvoiceExternalResponse voidInvoice(VoidInvoiceExternalRequest request) {
        log.info("[Mock] 作廢發票請求: invoiceNumber={}, reason={}",
                request.getInvoiceNumber(),
                request.getReason());

        // TODO: 實作正式發票服務後移除
        log.info("[Mock] 作廢發票成功: invoiceNumber={}", request.getInvoiceNumber());

        return VoidInvoiceExternalResponse.success();
    }

    @Override
    public AllowanceExternalResponse createAllowance(AllowanceExternalRequest request) {
        log.info("[Mock] 開立折讓請求: invoiceNumber={}, amount={}, reason={}",
                request.getInvoiceNumber(),
                request.getTotalAmount(),
                request.getReason());

        // TODO: 實作正式發票服務後移除
        String allowanceNumber = "AA-" + String.format("%08d", allowanceSequence.getAndIncrement());
        String externalId = "mock-allowance-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("[Mock] 開立折讓成功: allowanceNumber={}", allowanceNumber);

        return AllowanceExternalResponse.builder()
                .success(true)
                .allowanceNumber(allowanceNumber)
                .allowanceDate(LocalDate.now())
                .externalId(externalId)
                .resultCode("SUCCESS")
                .resultMessage("Mock 折讓開立成功")
                .build();
    }

    /**
     * 取得當前發票期別
     * 格式: 民國年 + 月份區間 (如 11312 = 113年11-12月)
     */
    private String getCurrentPeriod() {
        LocalDate now = LocalDate.now();
        int rocYear = now.getYear() - 1911;
        int month = now.getMonthValue();

        // 發票期別為雙月制: 1-2, 3-4, 5-6, 7-8, 9-10, 11-12
        int periodStart = ((month - 1) / 2) * 2 + 1;
        int periodEnd = periodStart + 1;

        return String.format("%d%02d", rocYear, periodEnd);
    }
}
