package com.morningharvest.erp.pos.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.pos.dto.SaleItemDTO;
import com.morningharvest.erp.pos.service.PosMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pos/menu")
@RequiredArgsConstructor
@Tag(name = "POS 菜單", description = "POS 點餐系統 - 銷售物品查詢")
public class PosMenuController {

    private final PosMenuService posMenuService;

    @GetMapping("/list")
    @Operation(summary = "查詢銷售物品列表", description = "查詢商品與套餐的混合列表（僅啟用/上架），回傳含 orderPayload 供直接下單")
    public ApiResponse<List<SaleItemDTO>> listSaleItems(
            @Parameter(description = "分類 ID（可選，不傳則查詢全部）")
            @RequestParam(value = "categoryId", required = false) Long categoryId
    ) {
        log.debug("POS 查詢銷售物品列表, categoryId: {}", categoryId);

        List<SaleItemDTO> result = posMenuService.listSaleItems(categoryId);
        return ApiResponse.success(result);
    }

    @GetMapping("/detail")
    @Operation(summary = "查詢銷售物品詳情", description = "查詢單一商品或套餐的詳細資訊，包含選項群組供客製化")
    public ApiResponse<SaleItemDTO> getSaleItemDetail(
            @Parameter(description = "類型：SINGLE (商品) 或 COMBO (套餐)", required = true, example = "SINGLE")
            @RequestParam("type") String type,

            @Parameter(description = "商品 ID 或套餐 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.debug("POS 查詢銷售物品詳情, type: {}, id: {}", type, id);

        SaleItemDTO result = posMenuService.getSaleItemDetail(type, id);
        return ApiResponse.success(result);
    }
}
