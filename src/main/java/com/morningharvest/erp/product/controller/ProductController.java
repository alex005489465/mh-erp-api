package com.morningharvest.erp.product.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.product.dto.CreateProductRequest;
import com.morningharvest.erp.product.dto.ProductDTO;
import com.morningharvest.erp.product.dto.UpdateProductRequest;
import com.morningharvest.erp.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 商品管理 Controller
 * 提供商品 CRUD 及上下架等 API
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "商品管理", description = "商品資料維護、上下架等操作")
public class ProductController {

    private final ProductService productService;

    /**
     * 取得商品詳情
     */
    @GetMapping("/detail")
    @Operation(summary = "取得商品詳情", description = "根據商品 ID 取得商品詳細資訊")
    public ApiResponse<ProductDTO> getProductDetail(
            @Parameter(description = "商品 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.debug("查詢商品詳情, id: {}", id);
        ProductDTO product = productService.getProductById(id);
        return ApiResponse.success(product);
    }

    /**
     * 分頁查詢商品列表
     */
    @GetMapping("/list")
    @Operation(summary = "查詢商品列表", description = "分頁查詢商品，可篩選上架狀態")
    public ApiResponse<PageResponse<ProductDTO>> listProducts(
            @Parameter(description = "頁碼 (從 1 開始)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") Integer page,

            @Parameter(description = "每頁筆數", example = "20")
            @RequestParam(value = "size", defaultValue = "20") Integer size,

            @Parameter(description = "排序欄位", example = "sortOrder")
            @RequestParam(value = "sortBy", defaultValue = "sortOrder") String sortBy,

            @Parameter(description = "排序方向 (ASC/DESC)", example = "ASC")
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction,

            @Parameter(description = "上架狀態篩選 (true=上架, false=下架, 不傳=全部)")
            @RequestParam(value = "isActive", required = false) Boolean isActive
    ) {
        log.debug("查詢商品列表, page: {}, size: {}, isActive: {}", page, size, isActive);

        PageableRequest pageableRequest = PageableRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PageResponse<ProductDTO> result = productService.listProducts(pageableRequest, isActive);
        return ApiResponse.success(result);
    }

    /**
     * 新增商品
     */
    @PostMapping("/create")
    @Operation(summary = "新增商品", description = "建立新商品")
    public ApiResponse<ProductDTO> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        log.info("新增商品: {}", request.getName());
        ProductDTO product = productService.createProduct(request);
        return ApiResponse.success("商品建立成功", product);
    }

    /**
     * 更新商品
     */
    @PostMapping("/update")
    @Operation(summary = "更新商品", description = "更新商品資料")
    public ApiResponse<ProductDTO> updateProduct(
            @Valid @RequestBody UpdateProductRequest request
    ) {
        log.info("更新商品, id: {}", request.getId());
        ProductDTO product = productService.updateProduct(request);
        return ApiResponse.success("商品更新成功", product);
    }

    /**
     * 刪除商品
     */
    @PostMapping("/delete")
    @Operation(summary = "刪除商品", description = "刪除指定商品")
    public ApiResponse<Void> deleteProduct(
            @Parameter(description = "商品 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("刪除商品, id: {}", id);
        productService.deleteProduct(id);
        return ApiResponse.success("商品刪除成功");
    }

    /**
     * 上架商品
     */
    @PostMapping("/activate")
    @Operation(summary = "上架商品", description = "將商品設為上架狀態")
    public ApiResponse<ProductDTO> activateProduct(
            @Parameter(description = "商品 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("上架商品, id: {}", id);
        ProductDTO product = productService.activateProduct(id);
        return ApiResponse.success("商品上架成功", product);
    }

    /**
     * 下架商品
     */
    @PostMapping("/deactivate")
    @Operation(summary = "下架商品", description = "將商品設為下架狀態")
    public ApiResponse<ProductDTO> deactivateProduct(
            @Parameter(description = "商品 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("下架商品, id: {}", id);
        ProductDTO product = productService.deactivateProduct(id);
        return ApiResponse.success("商品下架成功", product);
    }
}
