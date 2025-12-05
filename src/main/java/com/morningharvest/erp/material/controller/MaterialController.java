package com.morningharvest.erp.material.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.material.dto.CreateMaterialRequest;
import com.morningharvest.erp.material.dto.MaterialDTO;
import com.morningharvest.erp.material.dto.UpdateMaterialRequest;
import com.morningharvest.erp.material.service.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 原物料管理 Controller
 * 提供原物料 CRUD 及啟用停用等 API
 */
@Slf4j
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
@Tag(name = "原物料管理", description = "原物料主檔維護、啟用停用等操作")
public class MaterialController {

    private final MaterialService materialService;

    /**
     * 取得原物料詳情
     */
    @GetMapping("/detail")
    @Operation(summary = "取得原物料詳情", description = "根據原物料 ID 取得詳細資訊")
    public ApiResponse<MaterialDTO> getMaterialDetail(
            @Parameter(description = "原物料 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.debug("查詢原物料詳情, id: {}", id);
        MaterialDTO material = materialService.getMaterialById(id);
        return ApiResponse.success(material);
    }

    /**
     * 分頁查詢原物料列表
     */
    @GetMapping("/list")
    @Operation(summary = "查詢原物料列表", description = "分頁查詢原物料，可篩選啟用狀態、分類和關鍵字")
    public ApiResponse<PageResponse<MaterialDTO>> listMaterials(
            @Parameter(description = "頁碼 (從 1 開始)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") Integer page,

            @Parameter(description = "每頁筆數", example = "20")
            @RequestParam(value = "size", defaultValue = "20") Integer size,

            @Parameter(description = "排序欄位", example = "code")
            @RequestParam(value = "sortBy", defaultValue = "code") String sortBy,

            @Parameter(description = "排序方向 (ASC/DESC)", example = "ASC")
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction,

            @Parameter(description = "啟用狀態篩選 (true=啟用, false=停用, 不傳=全部)")
            @RequestParam(value = "isActive", required = false) Boolean isActive,

            @Parameter(description = "分類篩選 (BREAD, EGG, MEAT, BEVERAGE, SEASONING, DAIRY, VEGETABLE, FRUIT, OTHER)")
            @RequestParam(value = "category", required = false) String category,

            @Parameter(description = "關鍵字搜尋 (名稱模糊查詢)")
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        log.debug("查詢原物料列表, page: {}, size: {}, isActive: {}, category: {}, keyword: {}",
                page, size, isActive, category, keyword);

        PageableRequest pageableRequest = PageableRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PageResponse<MaterialDTO> result = materialService.listMaterials(
                pageableRequest, isActive, category, keyword);
        return ApiResponse.success(result);
    }

    /**
     * 新增原物料
     */
    @PostMapping("/create")
    @Operation(summary = "新增原物料", description = "建立新原物料")
    public ApiResponse<MaterialDTO> createMaterial(
            @Valid @RequestBody CreateMaterialRequest request
    ) {
        log.info("新增原物料: {}", request.getCode());
        MaterialDTO material = materialService.createMaterial(request);
        return ApiResponse.success("原物料建立成功", material);
    }

    /**
     * 更新原物料
     */
    @PostMapping("/update")
    @Operation(summary = "更新原物料", description = "更新原物料資料")
    public ApiResponse<MaterialDTO> updateMaterial(
            @Valid @RequestBody UpdateMaterialRequest request
    ) {
        log.info("更新原物料, id: {}", request.getId());
        MaterialDTO material = materialService.updateMaterial(request);
        return ApiResponse.success("原物料更新成功", material);
    }

    /**
     * 刪除（停用）原物料
     */
    @PostMapping("/delete")
    @Operation(summary = "刪除原物料", description = "停用指定原物料（軟刪除）")
    public ApiResponse<Void> deleteMaterial(
            @Parameter(description = "原物料 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("停用原物料, id: {}", id);
        materialService.deleteMaterial(id);
        return ApiResponse.success("原物料停用成功");
    }

    /**
     * 啟用原物料
     */
    @PostMapping("/activate")
    @Operation(summary = "啟用原物料", description = "將原物料設為啟用狀態")
    public ApiResponse<MaterialDTO> activateMaterial(
            @Parameter(description = "原物料 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("啟用原物料, id: {}", id);
        MaterialDTO material = materialService.activateMaterial(id);
        return ApiResponse.success("原物料啟用成功", material);
    }
}
