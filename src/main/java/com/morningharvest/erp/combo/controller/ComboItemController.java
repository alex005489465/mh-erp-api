package com.morningharvest.erp.combo.controller;

import com.morningharvest.erp.combo.dto.*;
import com.morningharvest.erp.combo.service.ComboItemService;
import com.morningharvest.erp.common.dto.ApiResponse;
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
@RequestMapping("/api/combos/items")
@RequiredArgsConstructor
@Tag(name = "套餐項目管理", description = "套餐內商品項目的新增、修改、刪除")
public class ComboItemController {

    private final ComboItemService comboItemService;

    @GetMapping("/list")
    @Operation(summary = "查詢套餐項目列表", description = "根據套餐 ID 查詢所有項目")
    public ApiResponse<List<ComboItemDTO>> listComboItems(
            @Parameter(description = "套餐 ID", required = true, example = "1")
            @RequestParam("comboId") Long comboId
    ) {
        log.debug("查詢套餐項目列表, comboId: {}", comboId);
        List<ComboItemDTO> items = comboItemService.listComboItemsByComboId(comboId);
        return ApiResponse.success(items);
    }

    @GetMapping("/detail")
    @Operation(summary = "取得套餐項目詳情", description = "根據項目 ID 取得詳細資訊")
    public ApiResponse<ComboItemDTO> getComboItemDetail(
            @Parameter(description = "項目 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.debug("查詢套餐項目詳情, id: {}", id);
        ComboItemDTO item = comboItemService.getComboItemById(id);
        return ApiResponse.success(item);
    }

    @PostMapping("/create")
    @Operation(summary = "新增套餐項目", description = "新增單一商品到套餐")
    public ApiResponse<ComboItemDTO> createComboItem(
            @Valid @RequestBody CreateComboItemRequest request
    ) {
        log.info("新增套餐項目: comboId={}, productId={}", request.getComboId(), request.getProductId());
        ComboItemDTO item = comboItemService.createComboItem(request);
        return ApiResponse.success("套餐項目新增成功", item);
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批次新增套餐項目", description = "一次新增多個商品到套餐")
    public ApiResponse<List<ComboItemDTO>> batchCreateComboItems(
            @Valid @RequestBody BatchCreateComboItemRequest request
    ) {
        log.info("批次新增套餐項目: comboId={}, itemCount={}", request.getComboId(), request.getItems().size());
        List<ComboItemDTO> items = comboItemService.batchCreateComboItems(request);
        return ApiResponse.success("套餐項目批次新增成功", items);
    }

    @PostMapping("/update")
    @Operation(summary = "更新套餐項目", description = "更新套餐項目資料（商品、數量、排序）")
    public ApiResponse<ComboItemDTO> updateComboItem(
            @Parameter(description = "項目 ID", required = true, example = "1")
            @RequestParam("id") Long id,
            @Valid @RequestBody UpdateComboItemRequest request
    ) {
        log.info("更新套餐項目, id: {}", id);
        ComboItemDTO item = comboItemService.updateComboItem(id, request);
        return ApiResponse.success("套餐項目更新成功", item);
    }

    @PostMapping("/delete")
    @Operation(summary = "刪除套餐項目", description = "從套餐中移除指定項目")
    public ApiResponse<Void> deleteComboItem(
            @Parameter(description = "項目 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("刪除套餐項目, id: {}", id);
        comboItemService.deleteComboItem(id);
        return ApiResponse.success("套餐項目刪除成功");
    }
}
