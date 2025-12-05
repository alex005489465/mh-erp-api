package com.morningharvest.erp.inventorycheck.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.inventorycheck.dto.*;
import com.morningharvest.erp.inventorycheck.service.InventoryCheckService;
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
 * 庫存盤點管理 API
 */
@Slf4j
@RestController
@RequestMapping("/api/inventory-checks")
@RequiredArgsConstructor
@Tag(name = "庫存盤點管理", description = "庫存盤點相關 API")
public class InventoryCheckController {

    private final InventoryCheckService inventoryCheckService;

    @GetMapping("/detail")
    @Operation(summary = "取得盤點單詳情",
               description = "依 ID 查詢盤點單詳情，包含所有盤點明細")
    public ApiResponse<InventoryCheckDetailDTO> getInventoryCheckDetail(
            @Parameter(description = "盤點單 ID", required = true)
            @RequestParam("id") Long id
    ) {
        log.debug("查詢盤點單詳情, id: {}", id);
        InventoryCheckDetailDTO detail = inventoryCheckService.getInventoryCheckById(id);
        return ApiResponse.success(detail);
    }

    @GetMapping("/list")
    @Operation(summary = "查詢盤點單列表",
               description = "分頁查詢盤點單，支援關鍵字、狀態、日期篩選")
    public ApiResponse<PageResponse<InventoryCheckDTO>> listInventoryChecks(
            @Parameter(description = "頁碼 (1-based)")
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @Parameter(description = "每頁筆數")
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @Parameter(description = "排序欄位")
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向 (ASC/DESC)")
            @RequestParam(value = "direction", defaultValue = "DESC") Sort.Direction direction,
            @Parameter(description = "關鍵字搜尋 (盤點單號)")
            @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "狀態篩選 (PLANNED/IN_PROGRESS/CONFIRMED)")
            @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "盤點日期起 (yyyy-MM-dd)")
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "盤點日期迄 (yyyy-MM-dd)")
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.debug("查詢盤點單列表, page: {}, size: {}", page, size);

        PageableRequest pageableRequest = PageableRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PageResponse<InventoryCheckDTO> result = inventoryCheckService.listInventoryChecks(
                pageableRequest, keyword, status, startDate, endDate);
        return ApiResponse.success(result);
    }

    @PostMapping("/create")
    @Operation(summary = "建立盤點計畫",
               description = "建立新的盤點計畫 (PLANNED 狀態)，自動載入所有啟用中的原物料")
    public ApiResponse<InventoryCheckDetailDTO> createInventoryCheck(
            @Valid @RequestBody CreateInventoryCheckRequest request
    ) {
        log.info("建立盤點計畫");
        InventoryCheckDetailDTO detail = inventoryCheckService.createInventoryCheck(request);
        return ApiResponse.success("盤點計畫已建立", detail);
    }

    @PostMapping("/start")
    @Operation(summary = "開始盤點",
               description = "開始盤點 (PLANNED -> IN_PROGRESS)")
    public ApiResponse<InventoryCheckDetailDTO> startInventoryCheck(
            @Valid @RequestBody StartInventoryCheckRequest request
    ) {
        log.info("開始盤點, id: {}", request.getId());
        InventoryCheckDetailDTO detail = inventoryCheckService.startInventoryCheck(request);
        return ApiResponse.success("盤點已開始", detail);
    }

    @PostMapping("/update-item")
    @Operation(summary = "更新盤點明細",
               description = "更新盤點項目的實際盤點數量")
    public ApiResponse<InventoryCheckItemDTO> updateInventoryCheckItem(
            @Valid @RequestBody UpdateInventoryCheckItemRequest request
    ) {
        log.info("更新盤點明細, itemId: {}", request.getItemId());
        InventoryCheckItemDTO item = inventoryCheckService.updateInventoryCheckItem(request);
        return ApiResponse.success("盤點明細已更新", item);
    }

    @PostMapping("/confirm")
    @Operation(summary = "確認盤點",
               description = "確認盤點 (IN_PROGRESS -> CONFIRMED)，觸發庫存調整")
    public ApiResponse<InventoryCheckDetailDTO> confirmInventoryCheck(
            @Valid @RequestBody ConfirmInventoryCheckRequest request
    ) {
        log.info("確認盤點, id: {}", request.getId());
        InventoryCheckDetailDTO detail = inventoryCheckService.confirmInventoryCheck(request);
        return ApiResponse.success("盤點已確認，庫存已調整", detail);
    }

    @PostMapping("/delete")
    @Operation(summary = "刪除盤點單",
               description = "刪除盤點單 (僅限 PLANNED 狀態)")
    public ApiResponse<Void> deleteInventoryCheck(
            @Parameter(description = "盤點單 ID", required = true)
            @RequestParam("id") Long id
    ) {
        log.info("刪除盤點單, id: {}", id);
        inventoryCheckService.deleteInventoryCheck(id);
        return ApiResponse.success("盤點單已刪除", null);
    }
}
