package com.morningharvest.erp.product.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.product.dto.*;
import com.morningharvest.erp.product.service.ProductOptionGroupService;
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
@RequestMapping("/api/products/options/groups")
@RequiredArgsConstructor
@Tag(name = "產品選項群組管理", description = "產品選項群組維護、啟用/停用等操作")
public class ProductOptionGroupController {

    private final ProductOptionGroupService groupService;

    @GetMapping("/detail")
    @Operation(summary = "取得產品選項群組詳情", description = "根據 ID 取得群組詳細資訊（含選項值）")
    public ApiResponse<ProductOptionGroupDetailDTO> getGroupDetail(
            @Parameter(description = "群組 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.debug("查詢產品選項群組詳情, id: {}", id);
        ProductOptionGroupDetailDTO group = groupService.getGroupDetailById(id);
        return ApiResponse.success(group);
    }

    @GetMapping("/list")
    @Operation(summary = "查詢產品選項群組列表", description = "查詢指定產品的所有選項群組")
    public ApiResponse<List<ProductOptionGroupDTO>> listGroups(
            @Parameter(description = "產品 ID", required = true, example = "1")
            @RequestParam("productId") Long productId
    ) {
        log.debug("查詢產品選項群組列表, productId: {}", productId);
        List<ProductOptionGroupDTO> groups = groupService.listGroupsByProductId(productId);
        return ApiResponse.success(groups);
    }

    @GetMapping("/list-with-values")
    @Operation(summary = "查詢產品選項群組列表（含選項值）", description = "查詢指定產品的所有選項群組，包含各群組下的選項值")
    public ApiResponse<List<ProductOptionGroupDetailDTO>> listGroupsWithValues(
            @Parameter(description = "產品 ID", required = true, example = "1")
            @RequestParam("productId") Long productId
    ) {
        log.debug("查詢產品選項群組列表（含選項值）, productId: {}", productId);
        List<ProductOptionGroupDetailDTO> groups = groupService.listGroupsWithValuesByProductId(productId);
        return ApiResponse.success(groups);
    }

    @PostMapping("/create")
    @Operation(summary = "新增產品選項群組", description = "為指定產品建立新選項群組")
    public ApiResponse<ProductOptionGroupDTO> createGroup(
            @Valid @RequestBody CreateProductOptionGroupRequest request
    ) {
        log.info("新增產品選項群組: productId={}, name={}", request.getProductId(), request.getName());
        ProductOptionGroupDTO group = groupService.createGroup(request);
        return ApiResponse.success("產品選項群組建立成功", group);
    }

    @PostMapping("/update")
    @Operation(summary = "更新產品選項群組", description = "更新產品選項群組資料")
    public ApiResponse<ProductOptionGroupDTO> updateGroup(
            @Valid @RequestBody UpdateProductOptionGroupRequest request
    ) {
        log.info("更新產品選項群組, id: {}", request.getId());
        ProductOptionGroupDTO group = groupService.updateGroup(request);
        return ApiResponse.success("產品選項群組更新成功", group);
    }

    @PostMapping("/delete")
    @Operation(summary = "刪除產品選項群組", description = "刪除指定產品選項群組（同時刪除群組下所有選項值）")
    public ApiResponse<Void> deleteGroup(
            @Parameter(description = "群組 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("刪除產品選項群組, id: {}", id);
        groupService.deleteGroup(id);
        return ApiResponse.success("產品選項群組刪除成功");
    }

    @PostMapping("/activate")
    @Operation(summary = "啟用產品選項群組", description = "將產品選項群組設為啟用狀態")
    public ApiResponse<ProductOptionGroupDTO> activateGroup(
            @Parameter(description = "群組 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("啟用產品選項群組, id: {}", id);
        ProductOptionGroupDTO group = groupService.activateGroup(id);
        return ApiResponse.success("產品選項群組啟用成功", group);
    }

    @PostMapping("/deactivate")
    @Operation(summary = "停用產品選項群組", description = "將產品選項群組設為停用狀態")
    public ApiResponse<ProductOptionGroupDTO> deactivateGroup(
            @Parameter(description = "群組 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("停用產品選項群組, id: {}", id);
        ProductOptionGroupDTO group = groupService.deactivateGroup(id);
        return ApiResponse.success("產品選項群組停用成功", group);
    }
}
