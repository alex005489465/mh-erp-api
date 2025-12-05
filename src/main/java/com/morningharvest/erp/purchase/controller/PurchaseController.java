package com.morningharvest.erp.purchase.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.purchase.dto.*;
import com.morningharvest.erp.purchase.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 進貨管理 API
 */
@Slf4j
@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
@Tag(name = "進貨管理", description = "進貨單維護相關 API")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping("/detail")
    @Operation(summary = "取得進貨單詳情", description = "根據進貨單 ID 取得詳細資訊，包含所有明細")
    public ApiResponse<PurchaseDetailDTO> getPurchaseDetail(
            @Parameter(description = "進貨單 ID", required = true)
            @RequestParam("id") Long id
    ) {
        log.debug("查詢進貨單詳情, id: {}", id);
        PurchaseDetailDTO purchase = purchaseService.getPurchaseById(id);
        return ApiResponse.success(purchase);
    }

    @GetMapping("/list")
    @Operation(summary = "查詢進貨單列表", description = "分頁查詢進貨單，支援多種篩選條件")
    public ApiResponse<PageResponse<PurchaseDTO>> listPurchases(
            @Parameter(description = "頁碼 (從 1 開始)")
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @Parameter(description = "每頁筆數")
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @Parameter(description = "排序欄位")
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向 (ASC/DESC)")
            @RequestParam(value = "direction", defaultValue = "DESC") Sort.Direction direction,
            @Parameter(description = "關鍵字搜尋 (進貨單號、供應商名稱)")
            @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "狀態篩選 (DRAFT/CONFIRMED)")
            @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "供應商ID篩選")
            @RequestParam(value = "supplierId", required = false) Long supplierId,
            @Parameter(description = "進貨日期開始 (yyyy-MM-dd)")
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "進貨日期結束 (yyyy-MM-dd)")
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.debug("查詢進貨單列表, page: {}, size: {}", page, size);

        PageableRequest pageableRequest = PageableRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PageResponse<PurchaseDTO> result = purchaseService.listPurchases(
                pageableRequest, keyword, status, supplierId, startDate, endDate);
        return ApiResponse.success(result);
    }

    @PostMapping("/create")
    @Operation(summary = "新增進貨單", description = "建立新的進貨單（草稿狀態）")
    public ApiResponse<PurchaseDetailDTO> createPurchase(
            @Valid @RequestBody CreatePurchaseRequest request
    ) {
        log.info("新增進貨單, supplierId: {}", request.getSupplierId());
        PurchaseDetailDTO purchase = purchaseService.createPurchase(request);
        return ApiResponse.success("進貨單建立成功", purchase);
    }

    @PostMapping("/update")
    @Operation(summary = "更新進貨單", description = "更新草稿狀態的進貨單")
    public ApiResponse<PurchaseDetailDTO> updatePurchase(
            @Valid @RequestBody UpdatePurchaseRequest request
    ) {
        log.info("更新進貨單, id: {}", request.getId());
        PurchaseDetailDTO purchase = purchaseService.updatePurchase(request);
        return ApiResponse.success("進貨單更新成功", purchase);
    }

    @PostMapping("/delete")
    @Operation(summary = "刪除進貨單", description = "刪除草稿狀態的進貨單")
    public ApiResponse<Void> deletePurchase(
            @Parameter(description = "進貨單 ID", required = true)
            @RequestParam("id") Long id
    ) {
        log.info("刪除進貨單, id: {}", id);
        purchaseService.deletePurchase(id);
        return ApiResponse.success("進貨單刪除成功");
    }

    @PostMapping("/confirm")
    @Operation(summary = "確認進貨單", description = "確認進貨單（DRAFT -> CONFIRMED），將觸發庫存更新")
    public ApiResponse<PurchaseDetailDTO> confirmPurchase(
            @Valid @RequestBody ConfirmPurchaseRequest request
    ) {
        log.info("確認進貨單, id: {}", request.getId());
        PurchaseDetailDTO purchase = purchaseService.confirmPurchase(request);
        return ApiResponse.success("進貨單確認成功，庫存已更新", purchase);
    }
}
