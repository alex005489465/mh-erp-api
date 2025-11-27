package com.morningharvest.erp.option.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.option.dto.*;
import com.morningharvest.erp.option.service.OptionTemplateGroupService;
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
@RequestMapping("/api/options/templates/groups")
@RequiredArgsConstructor
@Tag(name = "選項範本群組管理", description = "選項範本群組維護、啟用/停用等操作")
public class OptionTemplateGroupController {

    private final OptionTemplateGroupService groupService;

    @GetMapping("/detail")
    @Operation(summary = "取得選項範本群組詳情", description = "根據 ID 取得群組詳細資訊（含選項值）")
    public ApiResponse<OptionTemplateGroupDetailDTO> getGroupDetail(
            @Parameter(description = "群組 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.debug("查詢選項範本群組詳情, id: {}", id);
        OptionTemplateGroupDetailDTO group = groupService.getGroupDetailById(id);
        return ApiResponse.success(group);
    }

    @GetMapping("/list")
    @Operation(summary = "查詢選項範本群組列表", description = "分頁查詢選項範本群組")
    public ApiResponse<PageResponse<OptionTemplateGroupDTO>> listGroups(
            @Parameter(description = "頁碼 (從 1 開始)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") Integer page,

            @Parameter(description = "每頁筆數", example = "20")
            @RequestParam(value = "size", defaultValue = "20") Integer size,

            @Parameter(description = "排序欄位", example = "sortOrder")
            @RequestParam(value = "sortBy", defaultValue = "sortOrder") String sortBy,

            @Parameter(description = "排序方向 (ASC/DESC)", example = "ASC")
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction,

            @Parameter(description = "啟用狀態篩選 (true=啟用, false=停用, 不傳=全部)")
            @RequestParam(value = "isActive", required = false) Boolean isActive
    ) {
        log.debug("查詢選項範本群組列表, page: {}, size: {}, isActive: {}", page, size, isActive);

        PageableRequest pageableRequest = PageableRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PageResponse<OptionTemplateGroupDTO> result = groupService.listGroups(pageableRequest, isActive);
        return ApiResponse.success(result);
    }

    @PostMapping("/create")
    @Operation(summary = "新增選項範本群組", description = "建立新選項範本群組")
    public ApiResponse<OptionTemplateGroupDTO> createGroup(
            @Valid @RequestBody CreateOptionTemplateGroupRequest request
    ) {
        log.info("新增選項範本群組: {}", request.getName());
        OptionTemplateGroupDTO group = groupService.createGroup(request);
        return ApiResponse.success("選項範本群組建立成功", group);
    }

    @PostMapping("/update")
    @Operation(summary = "更新選項範本群組", description = "更新選項範本群組資料")
    public ApiResponse<OptionTemplateGroupDTO> updateGroup(
            @Valid @RequestBody UpdateOptionTemplateGroupRequest request
    ) {
        log.info("更新選項範本群組, id: {}", request.getId());
        OptionTemplateGroupDTO group = groupService.updateGroup(request);
        return ApiResponse.success("選項範本群組更新成功", group);
    }

    @PostMapping("/delete")
    @Operation(summary = "刪除選項範本群組", description = "刪除指定選項範本群組（同時刪除群組下所有選項值）")
    public ApiResponse<Void> deleteGroup(
            @Parameter(description = "群組 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("刪除選項範本群組, id: {}", id);
        groupService.deleteGroup(id);
        return ApiResponse.success("選項範本群組刪除成功");
    }

    @PostMapping("/activate")
    @Operation(summary = "啟用選項範本群組", description = "將選項範本群組設為啟用狀態")
    public ApiResponse<OptionTemplateGroupDTO> activateGroup(
            @Parameter(description = "群組 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("啟用選項範本群組, id: {}", id);
        OptionTemplateGroupDTO group = groupService.activateGroup(id);
        return ApiResponse.success("選項範本群組啟用成功", group);
    }

    @PostMapping("/deactivate")
    @Operation(summary = "停用選項範本群組", description = "將選項範本群組設為停用狀態")
    public ApiResponse<OptionTemplateGroupDTO> deactivateGroup(
            @Parameter(description = "群組 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("停用選項範本群組, id: {}", id);
        OptionTemplateGroupDTO group = groupService.deactivateGroup(id);
        return ApiResponse.success("選項範本群組停用成功", group);
    }
}
