package com.morningharvest.erp.supplier.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.supplier.dto.CreateSupplierRequest;
import com.morningharvest.erp.supplier.dto.SupplierDTO;
import com.morningharvest.erp.supplier.dto.UpdateSupplierRequest;
import com.morningharvest.erp.supplier.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 供應商管理 API
 */
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Tag(name = "供應商管理", description = "供應商維護相關 API")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping("/detail")
    @Operation(summary = "取得供應商詳情", description = "依據供應商 ID 取得詳細資料")
    public ApiResponse<SupplierDTO> getSupplierDetail(
            @Parameter(description = "供應商 ID", required = true)
            @RequestParam("id") Long id
    ) {
        SupplierDTO supplier = supplierService.getSupplierById(id);
        return ApiResponse.success(supplier);
    }

    @GetMapping("/list")
    @Operation(summary = "查詢供應商列表", description = "分頁查詢供應商列表，支援關鍵字搜尋和啟用狀態篩選")
    public ApiResponse<PageResponse<SupplierDTO>> listSuppliers(
            @Parameter(description = "頁碼 (從 1 開始)")
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @Parameter(description = "每頁筆數")
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @Parameter(description = "排序欄位")
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向 (ASC/DESC)")
            @RequestParam(value = "direction", defaultValue = "DESC") Sort.Direction direction,
            @Parameter(description = "關鍵字搜尋 (搜尋編號、名稱、簡稱、聯絡人)")
            @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "啟用狀態篩選")
            @RequestParam(value = "isActive", required = false) Boolean isActive
    ) {
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PageResponse<SupplierDTO> result = supplierService.listSuppliers(pageableRequest, keyword, isActive);
        return ApiResponse.success(result);
    }

    @PostMapping("/create")
    @Operation(summary = "新增供應商", description = "建立新的供應商資料")
    public ApiResponse<SupplierDTO> createSupplier(
            @Valid @RequestBody CreateSupplierRequest request
    ) {
        SupplierDTO supplier = supplierService.createSupplier(request);
        return ApiResponse.success("供應商建立成功", supplier);
    }

    @PostMapping("/update")
    @Operation(summary = "更新供應商", description = "更新現有供應商資料")
    public ApiResponse<SupplierDTO> updateSupplier(
            @Valid @RequestBody UpdateSupplierRequest request
    ) {
        SupplierDTO supplier = supplierService.updateSupplier(request);
        return ApiResponse.success("供應商更新成功", supplier);
    }

    @PostMapping("/delete")
    @Operation(summary = "刪除供應商", description = "刪除指定的供應商")
    public ApiResponse<Void> deleteSupplier(
            @Parameter(description = "供應商 ID", required = true)
            @RequestParam("id") Long id
    ) {
        supplierService.deleteSupplier(id);
        return ApiResponse.success("供應商刪除成功");
    }

    @PostMapping("/activate")
    @Operation(summary = "啟用供應商", description = "啟用指定的供應商")
    public ApiResponse<SupplierDTO> activateSupplier(
            @Parameter(description = "供應商 ID", required = true)
            @RequestParam("id") Long id
    ) {
        SupplierDTO supplier = supplierService.activateSupplier(id);
        return ApiResponse.success("供應商啟用成功", supplier);
    }

    @PostMapping("/deactivate")
    @Operation(summary = "停用供應商", description = "停用指定的供應商")
    public ApiResponse<SupplierDTO> deactivateSupplier(
            @Parameter(description = "供應商 ID", required = true)
            @RequestParam("id") Long id
    ) {
        SupplierDTO supplier = supplierService.deactivateSupplier(id);
        return ApiResponse.success("供應商停用成功", supplier);
    }
}
