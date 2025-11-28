package com.morningharvest.erp.combo.controller;

import com.morningharvest.erp.combo.dto.*;
import com.morningharvest.erp.combo.service.ComboService;
import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/combos")
@RequiredArgsConstructor
@Tag(name = "套餐管理", description = "套餐組合資料維護、啟停用等操作")
public class ComboController {

    private final ComboService comboService;

    @GetMapping("/detail")
    @Operation(summary = "取得套餐詳情", description = "根據套餐 ID 取得套餐詳細資訊（含項目與商品選項）")
    public ApiResponse<ComboDetailDTO> getComboDetail(
            @Parameter(description = "套餐 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.debug("查詢套餐詳情, id: {}", id);
        ComboDetailDTO combo = comboService.getComboDetailById(id);
        return ApiResponse.success(combo);
    }

    @GetMapping("/list")
    @Operation(summary = "查詢套餐列表", description = "分頁查詢套餐，可篩選啟用狀態和分類")
    public ApiResponse<PageResponse<ComboDTO>> listCombos(
            @Parameter(description = "頁碼 (從 1 開始)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") Integer page,

            @Parameter(description = "每頁筆數", example = "20")
            @RequestParam(value = "size", defaultValue = "20") Integer size,

            @Parameter(description = "排序欄位", example = "sortOrder")
            @RequestParam(value = "sortBy", defaultValue = "sortOrder") String sortBy,

            @Parameter(description = "排序方向 (ASC/DESC)", example = "ASC")
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction,

            @Parameter(description = "啟用狀態篩選 (true=啟用, false=停用, 不傳=全部)")
            @RequestParam(value = "isActive", required = false) Boolean isActive,

            @Parameter(description = "分類 ID 篩選")
            @RequestParam(value = "categoryId", required = false) Long categoryId
    ) {
        log.debug("查詢套餐列表, page: {}, size: {}, isActive: {}, categoryId: {}", page, size, isActive, categoryId);

        PageableRequest pageableRequest = PageableRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PageResponse<ComboDTO> result = comboService.listCombos(pageableRequest, isActive, categoryId);
        return ApiResponse.success(result);
    }

    @PostMapping("/create")
    @Operation(summary = "新增套餐", description = "建立新套餐")
    public ApiResponse<ComboDTO> createCombo(
            @Valid @RequestBody CreateComboRequest request
    ) {
        log.info("新增套餐: {}", request.getName());
        ComboDTO combo = comboService.createCombo(request);
        return ApiResponse.success("套餐建立成功", combo);
    }

    @PostMapping("/update")
    @Operation(summary = "更新套餐", description = "更新套餐資料")
    public ApiResponse<ComboDTO> updateCombo(
            @Parameter(description = "套餐 ID", required = true, example = "1")
            @RequestParam("id") Long id,
            @Valid @RequestBody UpdateComboRequest request
    ) {
        log.info("更新套餐, id: {}", id);
        ComboDTO combo = comboService.updateCombo(id, request);
        return ApiResponse.success("套餐更新成功", combo);
    }

    @PostMapping("/delete")
    @Operation(summary = "刪除套餐", description = "刪除指定套餐（同時刪除套餐項目）")
    public ApiResponse<Void> deleteCombo(
            @Parameter(description = "套餐 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("刪除套餐, id: {}", id);
        comboService.deleteCombo(id);
        return ApiResponse.success("套餐刪除成功");
    }

    @PostMapping("/activate")
    @Operation(summary = "啟用套餐", description = "將套餐設為啟用狀態")
    public ApiResponse<ComboDTO> activateCombo(
            @Parameter(description = "套餐 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("啟用套餐, id: {}", id);
        ComboDTO combo = comboService.activateCombo(id);
        return ApiResponse.success("套餐啟用成功", combo);
    }

    @PostMapping("/deactivate")
    @Operation(summary = "停用套餐", description = "將套餐設為停用狀態")
    public ApiResponse<ComboDTO> deactivateCombo(
            @Parameter(description = "套餐 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("停用套餐, id: {}", id);
        ComboDTO combo = comboService.deactivateCombo(id);
        return ApiResponse.success("套餐停用成功", combo);
    }
}
