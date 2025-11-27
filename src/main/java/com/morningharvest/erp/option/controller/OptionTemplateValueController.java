package com.morningharvest.erp.option.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.option.dto.*;
import com.morningharvest.erp.option.service.OptionTemplateValueService;
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
@RequestMapping("/api/options/templates/values")
@RequiredArgsConstructor
@Tag(name = "選項範本值管理", description = "選項範本值維護操作")
public class OptionTemplateValueController {

    private final OptionTemplateValueService valueService;

    @GetMapping("/list")
    @Operation(summary = "查詢群組下的選項值列表", description = "根據群組 ID 查詢所有選項值")
    public ApiResponse<List<OptionTemplateValueDTO>> listValues(
            @Parameter(description = "群組 ID", required = true, example = "1")
            @RequestParam("groupId") Long groupId
    ) {
        log.debug("查詢群組下的選項值列表, groupId: {}", groupId);
        List<OptionTemplateValueDTO> values = valueService.listValuesByGroupId(groupId);
        return ApiResponse.success(values);
    }

    @PostMapping("/create")
    @Operation(summary = "新增選項範本值", description = "在指定群組下建立新選項值")
    public ApiResponse<OptionTemplateValueDTO> createValue(
            @Valid @RequestBody CreateOptionTemplateValueRequest request
    ) {
        log.info("新增選項範本值: groupId={}, name={}", request.getGroupId(), request.getName());
        OptionTemplateValueDTO value = valueService.createValue(request);
        return ApiResponse.success("選項範本值建立成功", value);
    }

    @PostMapping("/update")
    @Operation(summary = "更新選項範本值", description = "更新選項範本值資料")
    public ApiResponse<OptionTemplateValueDTO> updateValue(
            @Valid @RequestBody UpdateOptionTemplateValueRequest request
    ) {
        log.info("更新選項範本值, id: {}", request.getId());
        OptionTemplateValueDTO value = valueService.updateValue(request);
        return ApiResponse.success("選項範本值更新成功", value);
    }

    @PostMapping("/delete")
    @Operation(summary = "刪除選項範本值", description = "刪除指定選項範本值")
    public ApiResponse<Void> deleteValue(
            @Parameter(description = "選項 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("刪除選項範本值, id: {}", id);
        valueService.deleteValue(id);
        return ApiResponse.success("選項範本值刪除成功");
    }

    @PostMapping("/activate")
    @Operation(summary = "啟用選項範本值", description = "將選項範本值設為啟用狀態")
    public ApiResponse<OptionTemplateValueDTO> activateValue(
            @Parameter(description = "選項 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("啟用選項範本值, id: {}", id);
        OptionTemplateValueDTO value = valueService.activateValue(id);
        return ApiResponse.success("選項範本值啟用成功", value);
    }

    @PostMapping("/deactivate")
    @Operation(summary = "停用選項範本值", description = "將選項範本值設為停用狀態")
    public ApiResponse<OptionTemplateValueDTO> deactivateValue(
            @Parameter(description = "選項 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("停用選項範本值, id: {}", id);
        OptionTemplateValueDTO value = valueService.deactivateValue(id);
        return ApiResponse.success("選項範本值停用成功", value);
    }
}
