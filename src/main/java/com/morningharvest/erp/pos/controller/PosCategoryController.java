package com.morningharvest.erp.pos.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.product.dto.ProductCategoryDTO;
import com.morningharvest.erp.product.service.ProductCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pos/categories")
@RequiredArgsConstructor
@Tag(name = "POS 分類", description = "POS 點餐系統 - 商品分類查詢")
public class PosCategoryController {

    private final ProductCategoryService productCategoryService;

    @GetMapping("/list")
    @Operation(summary = "查詢啟用的分類列表", description = "查詢所有啟用中的商品分類（供 POS 點餐使用）")
    public ApiResponse<PageResponse<ProductCategoryDTO>> listCategories(
            @Parameter(description = "頁碼 (從 1 開始)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") Integer page,

            @Parameter(description = "每頁筆數", example = "50")
            @RequestParam(value = "size", defaultValue = "50") Integer size,

            @Parameter(description = "排序欄位", example = "sortOrder")
            @RequestParam(value = "sortBy", defaultValue = "sortOrder") String sortBy,

            @Parameter(description = "排序方向 (ASC/DESC)", example = "ASC")
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction
    ) {
        log.debug("POS 查詢分類列表, page: {}, size: {}", page, size);

        PageableRequest pageableRequest = PageableRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        // POS 預設只查詢啟用的分類
        PageResponse<ProductCategoryDTO> result = productCategoryService.listCategories(pageableRequest, true);
        return ApiResponse.success(result);
    }
}
