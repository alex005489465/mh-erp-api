package com.morningharvest.erp.product.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.product.dto.CreateProductCategoryRequest;
import com.morningharvest.erp.product.dto.ProductCategoryDTO;
import com.morningharvest.erp.product.dto.UpdateProductCategoryRequest;
import com.morningharvest.erp.product.service.ProductCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 商品分類管理 Controller
 * 提供商品分類 CRUD 及啟用/停用等 API
 */
@Slf4j
@RestController
@RequestMapping("/api/products/categories")
@RequiredArgsConstructor
@Tag(name = "商品分類管理", description = "商品分類維護、啟用/停用等操作")
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    /**
     * 取得商品分類詳情
     */
    @GetMapping("/detail")
    @Operation(summary = "取得商品分類詳情", description = "根據分類 ID 取得分類詳細資訊")
    public ApiResponse<ProductCategoryDTO> getCategoryDetail(
            @Parameter(description = "分類 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.debug("查詢商品分類詳情, id: {}", id);
        ProductCategoryDTO category = productCategoryService.getCategoryById(id);
        return ApiResponse.success(category);
    }

    /**
     * 分頁查詢商品分類列表
     */
    @GetMapping("/list")
    @Operation(summary = "查詢商品分類列表", description = "分頁查詢商品分類，可篩選啟用狀態")
    public ApiResponse<PageResponse<ProductCategoryDTO>> listCategories(
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
        log.debug("查詢商品分類列表, page: {}, size: {}, isActive: {}", page, size, isActive);

        PageableRequest pageableRequest = PageableRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PageResponse<ProductCategoryDTO> result = productCategoryService.listCategories(pageableRequest, isActive);
        return ApiResponse.success(result);
    }

    /**
     * 新增商品分類
     */
    @PostMapping("/create")
    @Operation(summary = "新增商品分類", description = "建立新商品分類")
    public ApiResponse<ProductCategoryDTO> createCategory(
            @Valid @RequestBody CreateProductCategoryRequest request
    ) {
        log.info("新增商品分類: {}", request.getName());
        ProductCategoryDTO category = productCategoryService.createCategory(request);
        return ApiResponse.success("商品分類建立成功", category);
    }

    /**
     * 更新商品分類
     */
    @PostMapping("/update")
    @Operation(summary = "更新商品分類", description = "更新商品分類資料")
    public ApiResponse<ProductCategoryDTO> updateCategory(
            @Valid @RequestBody UpdateProductCategoryRequest request
    ) {
        log.info("更新商品分類, id: {}", request.getId());
        ProductCategoryDTO category = productCategoryService.updateCategory(request);
        return ApiResponse.success("商品分類更新成功", category);
    }

    /**
     * 刪除商品分類
     */
    @PostMapping("/delete")
    @Operation(summary = "刪除商品分類", description = "刪除指定商品分類")
    public ApiResponse<Void> deleteCategory(
            @Parameter(description = "分類 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("刪除商品分類, id: {}", id);
        productCategoryService.deleteCategory(id);
        return ApiResponse.success("商品分類刪除成功");
    }

    /**
     * 啟用商品分類
     */
    @PostMapping("/activate")
    @Operation(summary = "啟用商品分類", description = "將商品分類設為啟用狀態")
    public ApiResponse<ProductCategoryDTO> activateCategory(
            @Parameter(description = "分類 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("啟用商品分類, id: {}", id);
        ProductCategoryDTO category = productCategoryService.activateCategory(id);
        return ApiResponse.success("商品分類啟用成功", category);
    }

    /**
     * 停用商品分類
     */
    @PostMapping("/deactivate")
    @Operation(summary = "停用商品分類", description = "將商品分類設為停用狀態")
    public ApiResponse<ProductCategoryDTO> deactivateCategory(
            @Parameter(description = "分類 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("停用商品分類, id: {}", id);
        ProductCategoryDTO category = productCategoryService.deactivateCategory(id);
        return ApiResponse.success("商品分類停用成功", category);
    }
}
