package com.morningharvest.erp.product.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.product.dto.*;
import com.morningharvest.erp.product.service.ProductOptionValueService;
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
@RequestMapping("/api/products/options/values")
@RequiredArgsConstructor
@Tag(name = "產品選項值管理", description = "產品選項值維護操作")
public class ProductOptionValueController {

    private final ProductOptionValueService valueService;

    @GetMapping("/list")
    @Operation(summary = "查詢群組下的選項值列表", description = "根據群組 ID 查詢所有選項值")
    public ApiResponse<List<ProductOptionValueDTO>> listValues(
            @Parameter(description = "群組 ID", required = true, example = "1")
            @RequestParam("groupId") Long groupId
    ) {
        log.debug("查詢群組下的選項值列表, groupId: {}", groupId);
        List<ProductOptionValueDTO> values = valueService.listValuesByGroupId(groupId);
        return ApiResponse.success(values);
    }

    @PostMapping("/create")
    @Operation(summary = "新增產品選項值", description = "在指定群組下建立新選項值")
    public ApiResponse<ProductOptionValueDTO> createValue(
            @Valid @RequestBody CreateProductOptionValueRequest request
    ) {
        log.info("新增產品選項值: groupId={}, name={}", request.getGroupId(), request.getName());
        ProductOptionValueDTO value = valueService.createValue(request);
        return ApiResponse.success("產品選項值建立成功", value);
    }

    @PostMapping("/update")
    @Operation(summary = "更新產品選項值", description = "更新產品選項值資料")
    public ApiResponse<ProductOptionValueDTO> updateValue(
            @Valid @RequestBody UpdateProductOptionValueRequest request
    ) {
        log.info("更新產品選項值, id: {}", request.getId());
        ProductOptionValueDTO value = valueService.updateValue(request);
        return ApiResponse.success("產品選項值更新成功", value);
    }

    @PostMapping("/delete")
    @Operation(summary = "刪除產品選項值", description = "刪除指定產品選項值")
    public ApiResponse<Void> deleteValue(
            @Parameter(description = "選項 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("刪除產品選項值, id: {}", id);
        valueService.deleteValue(id);
        return ApiResponse.success("產品選項值刪除成功");
    }

    @PostMapping("/activate")
    @Operation(summary = "啟用產品選項值", description = "將產品選項值設為啟用狀態")
    public ApiResponse<ProductOptionValueDTO> activateValue(
            @Parameter(description = "選項 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("啟用產品選項值, id: {}", id);
        ProductOptionValueDTO value = valueService.activateValue(id);
        return ApiResponse.success("產品選項值啟用成功", value);
    }

    @PostMapping("/deactivate")
    @Operation(summary = "停用產品選項值", description = "將產品選項值設為停用狀態")
    public ApiResponse<ProductOptionValueDTO> deactivateValue(
            @Parameter(description = "選項 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("停用產品選項值, id: {}", id);
        ProductOptionValueDTO value = valueService.deactivateValue(id);
        return ApiResponse.success("產品選項值停用成功", value);
    }
}
