package com.morningharvest.erp.order.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.order.dto.*;
import com.morningharvest.erp.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 訂單管理 Controller
 * 提供訂單 CRUD 操作（整批更新模式）
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "訂單管理", description = "訂單建立、更新、刪除、完成等操作")
public class OrderController {

    private final OrderService orderService;

    /**
     * 建立訂單（含項目）
     */
    @PostMapping("/create")
    @Operation(summary = "建立訂單", description = "建立訂單並加入項目。可同時傳入單點商品和套餐")
    public ApiResponse<OrderDetailDTO> createOrder(
            @Valid @RequestBody(required = false) CreateOrderRequest request
    ) {
        log.info("建立訂單");
        if (request == null) {
            request = new CreateOrderRequest();
        }
        OrderDetailDTO order = orderService.createOrder(request);
        return ApiResponse.success("訂單建立成功", order);
    }

    /**
     * 更新訂單（整批取代項目）
     */
    @PostMapping("/update")
    @Operation(summary = "更新訂單", description = "整批更新訂單。會刪除所有舊項目，建立新項目")
    public ApiResponse<OrderDetailDTO> updateOrder(
            @Parameter(description = "訂單 ID", required = true, example = "1")
            @RequestParam("id") Long id,
            @Valid @RequestBody UpdateOrderRequest request
    ) {
        log.info("更新訂單, id: {}", id);
        OrderDetailDTO order = orderService.updateOrder(id, request);
        return ApiResponse.success("訂單更新成功", order);
    }

    /**
     * 刪除訂單
     */
    @PostMapping("/delete")
    @Operation(summary = "刪除訂單", description = "刪除草稿訂單及其所有項目")
    public ApiResponse<Void> deleteOrder(
            @Parameter(description = "訂單 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("刪除訂單, id: {}", id);
        orderService.deleteOrder(id);
        return ApiResponse.success("訂單已刪除");
    }

    /**
     * 完成訂單
     */
    @PostMapping("/complete")
    @Operation(summary = "完成訂單", description = "將草稿訂單設為完成狀態，完成後不可修改")
    public ApiResponse<OrderDTO> completeOrder(
            @Parameter(description = "訂單 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("完成訂單, id: {}", id);
        OrderDTO order = orderService.completeOrder(id);
        return ApiResponse.success("訂單完成", order);
    }

    /**
     * 取消訂單
     */
    @PostMapping("/cancel")
    @Operation(summary = "取消訂單", description = "取消訂單。已付款訂單會自動建立退款記錄。已完成訂單無法取消")
    public ApiResponse<CancelOrderResult> cancelOrder(
            @Parameter(description = "訂單 ID", required = true, example = "1")
            @RequestParam("id") Long id,
            @RequestBody(required = false) CancelOrderRequest request
    ) {
        log.info("取消訂單, id: {}", id);
        String reason = request != null ? request.getReason() : null;
        CancelOrderResult result = orderService.cancelOrder(id, reason);
        return ApiResponse.success("訂單取消成功", result);
    }

    /**
     * 取得訂單詳情
     */
    @GetMapping("/detail")
    @Operation(summary = "取得訂單詳情", description = "根據訂單 ID 取得訂單詳細資訊，包含所有項目")
    public ApiResponse<OrderDetailDTO> getOrderDetail(
            @Parameter(description = "訂單 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.debug("查詢訂單詳情, id: {}", id);
        OrderDetailDTO order = orderService.getOrderById(id);
        return ApiResponse.success(order);
    }

    /**
     * 分頁查詢訂單列表
     */
    @GetMapping("/list")
    @Operation(summary = "查詢訂單列表", description = "分頁查詢訂單，可篩選狀態和類型")
    public ApiResponse<PageResponse<OrderDTO>> listOrders(
            @Parameter(description = "頁碼 (從 1 開始)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") Integer page,

            @Parameter(description = "每頁筆數", example = "20")
            @RequestParam(value = "size", defaultValue = "20") Integer size,

            @Parameter(description = "排序欄位", example = "createdAt")
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,

            @Parameter(description = "排序方向 (ASC/DESC)", example = "DESC")
            @RequestParam(value = "direction", defaultValue = "DESC") Sort.Direction direction,

            @Parameter(description = "訂單狀態篩選 (DRAFT/COMPLETED)")
            @RequestParam(value = "status", required = false) String status,

            @Parameter(description = "訂單類型篩選 (DINE_IN/TAKEOUT/DELIVERY)")
            @RequestParam(value = "orderType", required = false) String orderType
    ) {
        log.debug("查詢訂單列表, page: {}, size: {}, status: {}, orderType: {}", page, size, status, orderType);

        PageableRequest pageableRequest = PageableRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PageResponse<OrderDTO> result = orderService.listOrders(pageableRequest, status, orderType);
        return ApiResponse.success(result);
    }
}
