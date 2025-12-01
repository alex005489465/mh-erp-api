package com.morningharvest.erp.pos.controller;

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

@Slf4j
@RestController
@RequestMapping("/api/pos/orders")
@RequiredArgsConstructor
@Tag(name = "POS 點餐", description = "POS 點餐系統 - 訂單操作")
public class PosOrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    @Operation(summary = "建立訂單", description = "建立 POS 訂單。可同時傳入單點商品和套餐")
    public ApiResponse<OrderDetailDTO> createOrder(
            @Valid @RequestBody(required = false) CreateOrderRequest request
    ) {
        log.info("POS 建立訂單");
        if (request == null) {
            request = new CreateOrderRequest();
        }
        OrderDetailDTO order = orderService.createOrder(request);
        return ApiResponse.success("訂單建立成功", order);
    }

    @PostMapping("/update")
    @Operation(summary = "更新訂單", description = "整批更新 POS 訂單。會刪除所有舊項目，建立新項目")
    public ApiResponse<OrderDetailDTO> updateOrder(
            @Parameter(description = "訂單 ID", required = true, example = "1")
            @RequestParam("id") Long id,
            @Valid @RequestBody UpdateOrderRequest request
    ) {
        log.info("POS 更新訂單, id: {}", id);
        OrderDetailDTO order = orderService.updateOrder(id, request);
        return ApiResponse.success("訂單更新成功", order);
    }

    @PostMapping("/delete")
    @Operation(summary = "刪除訂單", description = "刪除草稿 POS 訂單及其所有項目")
    public ApiResponse<Void> deleteOrder(
            @Parameter(description = "訂單 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("POS 刪除訂單, id: {}", id);
        orderService.deleteOrder(id);
        return ApiResponse.success("訂單已刪除");
    }

    @PostMapping("/submit")
    @Operation(summary = "送出訂單", description = "送出 POS 訂單，狀態變更為待付款 (DRAFT → PENDING_PAYMENT)。送出後不可修改，會自動建立付款條目")
    public ApiResponse<OrderDTO> submitOrder(
            @Parameter(description = "訂單 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("POS 送出訂單, id: {}", id);
        OrderDTO order = orderService.submitOrder(id);
        return ApiResponse.success("訂單已送出", order);
    }

    @PostMapping("/complete")
    @Operation(summary = "完成訂單", description = "完成 POS 訂單，狀態變更為已完成 (PAID → COMPLETED)。僅已付款訂單可完成")
    public ApiResponse<OrderDTO> completeOrder(
            @Parameter(description = "訂單 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("POS 完成訂單, id: {}", id);
        OrderDTO order = orderService.completeOrder(id);
        return ApiResponse.success("訂單完成", order);
    }

    @GetMapping("/detail")
    @Operation(summary = "查詢訂單詳情", description = "查詢 POS 訂單詳細資訊，包含所有項目")
    public ApiResponse<OrderDetailDTO> getOrderDetail(
            @Parameter(description = "訂單 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.debug("POS 查詢訂單詳情, id: {}", id);
        OrderDetailDTO order = orderService.getOrderById(id);
        return ApiResponse.success(order);
    }

    @GetMapping("/list")
    @Operation(summary = "查詢訂單列表", description = "分頁查詢 POS 訂單，預設查詢草稿訂單")
    public ApiResponse<PageResponse<OrderDTO>> listOrders(
            @Parameter(description = "頁碼 (從 1 開始)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") Integer page,

            @Parameter(description = "每頁筆數", example = "20")
            @RequestParam(value = "size", defaultValue = "20") Integer size,

            @Parameter(description = "排序欄位", example = "createdAt")
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,

            @Parameter(description = "排序方向 (ASC/DESC)", example = "DESC")
            @RequestParam(value = "direction", defaultValue = "DESC") Sort.Direction direction,

            @Parameter(description = "訂單狀態篩選 (DRAFT/COMPLETED)，預設 DRAFT")
            @RequestParam(value = "status", defaultValue = "DRAFT") String status,

            @Parameter(description = "訂單類型篩選 (DINE_IN/TAKEOUT/DELIVERY)")
            @RequestParam(value = "orderType", required = false) String orderType
    ) {
        log.debug("POS 查詢訂單列表, page: {}, size: {}, status: {}, orderType: {}", page, size, status, orderType);

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
