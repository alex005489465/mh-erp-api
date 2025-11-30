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
 * 提供訂單 CRUD 及項目管理 API
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "訂單管理", description = "點餐、訂單項目管理等操作")
public class OrderController {

    private final OrderService orderService;

    /**
     * 建立訂單（草稿）
     */
    @PostMapping("/create")
    @Operation(summary = "建立訂單", description = "建立新的草稿訂單")
    public ApiResponse<OrderDTO> createOrder(
            @Valid @RequestBody(required = false) CreateOrderRequest request
    ) {
        log.info("建立訂單");
        if (request == null) {
            request = new CreateOrderRequest();
        }
        OrderDTO order = orderService.createOrder(request);
        return ApiResponse.success("訂單建立成功", order);
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

    /**
     * 完成訂單
     */
    @PostMapping("/complete")
    @Operation(summary = "完成訂單", description = "將草稿訂單設為完成狀態")
    public ApiResponse<OrderDTO> completeOrder(
            @Parameter(description = "訂單 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("完成訂單, id: {}", id);
        OrderDTO order = orderService.completeOrder(id);
        return ApiResponse.success("訂單完成", order);
    }

    // ========== 訂單項目操作 ==========

    /**
     * 加入項目到訂單（單點商品或套餐）
     * - 單點商品: 設定 productId，回傳單一 OrderItemDTO
     * - 套餐: 設定 comboId，回傳 List<OrderItemDTO>
     */
    @PostMapping("/items/add")
    @Operation(summary = "加入項目", description = "將單點商品或套餐加入到訂單中。單點設定 productId，套餐設定 comboId")
    public ApiResponse<Object> addItem(
            @Valid @RequestBody AddItemRequest request
    ) {
        if (request.getComboId() != null) {
            log.info("加入套餐到訂單, orderId: {}, comboId: {}", request.getOrderId(), request.getComboId());
        } else {
            log.info("加入單點商品到訂單, orderId: {}, productId: {}", request.getOrderId(), request.getProductId());
        }
        Object result = orderService.addItem(request);
        String message = (request.getComboId() != null) ? "套餐已加入訂單" : "商品已加入訂單";
        return ApiResponse.success(message, result);
    }

    /**
     * 更新訂單項目
     */
    @PostMapping("/items/update")
    @Operation(summary = "更新項目", description = "更新訂單項目的數量、選項或備註")
    public ApiResponse<OrderItemDTO> updateItem(
            @Valid @RequestBody UpdateItemRequest request
    ) {
        log.info("更新訂單項目, itemId: {}", request.getItemId());
        OrderItemDTO item = orderService.updateItem(request);
        return ApiResponse.success("項目已更新", item);
    }

    /**
     * 移除訂單項目
     * - 單點商品: 只刪除該項目
     * - 套餐商品: 自動刪除整組套餐 (同一 groupSequence 的所有項目)
     */
    @PostMapping("/items/remove")
    @Operation(summary = "移除項目", description = "從訂單中移除項目。單點商品只刪除該項目，套餐商品會自動刪除整組")
    public ApiResponse<Void> removeItem(
            @Parameter(description = "項目 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("移除訂單項目, id: {}", id);
        orderService.removeItem(id);
        return ApiResponse.success("項目已移除");
    }
}
