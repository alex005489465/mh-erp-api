package com.morningharvest.erp.order.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.order.dto.OrderDTO;
import com.morningharvest.erp.order.dto.OrderDetailDTO;
import com.morningharvest.erp.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 訂單管理 Controller
 * 提供 ERP 訂單查詢功能
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "訂單管理", description = "ERP 訂單查詢")
public class OrderController {

    private final OrderService orderService;

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
