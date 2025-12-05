package com.morningharvest.erp.product.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.product.dto.CreateProductRecipeRequest;
import com.morningharvest.erp.product.dto.ProductRecipeDTO;
import com.morningharvest.erp.product.dto.UpdateProductRecipeRequest;
import com.morningharvest.erp.product.service.ProductRecipeService;
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
@RequestMapping("/api/products/recipes")
@RequiredArgsConstructor
@Tag(name = "商品配方管理", description = "管理商品與原物料的配方關聯")
public class ProductRecipeController {

    private final ProductRecipeService productRecipeService;

    @GetMapping("/detail")
    @Operation(summary = "取得配方詳情", description = "根據配方 ID 取得詳細資訊")
    public ApiResponse<ProductRecipeDTO> getRecipeDetail(
            @Parameter(description = "配方 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.debug("查詢配方詳情, id: {}", id);
        ProductRecipeDTO recipe = productRecipeService.getRecipeById(id);
        return ApiResponse.success(recipe);
    }

    @GetMapping("/list")
    @Operation(summary = "查詢商品配方清單", description = "根據商品 ID 查詢其配方清單")
    public ApiResponse<List<ProductRecipeDTO>> listRecipes(
            @Parameter(description = "商品 ID", required = true, example = "1")
            @RequestParam("productId") Long productId
    ) {
        log.debug("查詢商品配方清單, productId: {}", productId);
        List<ProductRecipeDTO> recipes = productRecipeService.listRecipesByProductId(productId);
        return ApiResponse.success(recipes);
    }

    @PostMapping("/create")
    @Operation(summary = "新增配方項目", description = "為商品新增原物料配方")
    public ApiResponse<ProductRecipeDTO> createRecipe(
            @Valid @RequestBody CreateProductRecipeRequest request
    ) {
        log.info("新增配方, productId: {}, materialId: {}", request.getProductId(), request.getMaterialId());
        ProductRecipeDTO recipe = productRecipeService.createRecipe(request);
        return ApiResponse.success("配方新增成功", recipe);
    }

    @PostMapping("/update")
    @Operation(summary = "更新配方項目", description = "更新配方的用量或備註")
    public ApiResponse<ProductRecipeDTO> updateRecipe(
            @Valid @RequestBody UpdateProductRecipeRequest request
    ) {
        log.info("更新配方, id: {}", request.getId());
        ProductRecipeDTO recipe = productRecipeService.updateRecipe(request);
        return ApiResponse.success("配方更新成功", recipe);
    }

    @PostMapping("/delete")
    @Operation(summary = "刪除配方項目", description = "刪除指定的配方項目")
    public ApiResponse<Void> deleteRecipe(
            @Parameter(description = "配方 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("刪除配方, id: {}", id);
        productRecipeService.deleteRecipe(id);
        return ApiResponse.success("配方刪除成功");
    }
}
