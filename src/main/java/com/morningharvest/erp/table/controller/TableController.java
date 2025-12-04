package com.morningharvest.erp.table.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.table.dto.CreateTableRequest;
import com.morningharvest.erp.table.dto.TableDTO;
import com.morningharvest.erp.table.dto.UpdateTableRequest;
import com.morningharvest.erp.table.service.TableService;
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
@RequestMapping("/api/tables")
@RequiredArgsConstructor
@Tag(name = "桌位管理", description = "桌位維護、CRUD 等操作")
public class TableController {

    private final TableService tableService;

    @GetMapping("/detail")
    @Operation(summary = "取得桌位詳情", description = "根據桌位 ID 取得桌位詳細資訊")
    public ApiResponse<TableDTO> getTableDetail(
            @Parameter(description = "桌位 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.debug("查詢桌位詳情, id: {}", id);
        TableDTO table = tableService.getTableById(id);
        return ApiResponse.success(table);
    }

    @GetMapping("/list")
    @Operation(summary = "查詢桌位列表", description = "分頁查詢桌位，可篩選狀態")
    public ApiResponse<PageResponse<TableDTO>> listTables(
            @Parameter(description = "頁碼 (從 1 開始)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") Integer page,

            @Parameter(description = "每頁筆數", example = "20")
            @RequestParam(value = "size", defaultValue = "20") Integer size,

            @Parameter(description = "排序欄位", example = "tableNumber")
            @RequestParam(value = "sortBy", defaultValue = "tableNumber") String sortBy,

            @Parameter(description = "排序方向 (ASC/DESC)", example = "ASC")
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction,

            @Parameter(description = "狀態篩選 (AVAILABLE/OCCUPIED)")
            @RequestParam(value = "status", required = false) String status
    ) {
        log.debug("查詢桌位列表, page: {}, size: {}, status: {}", page, size, status);

        PageableRequest pageableRequest = PageableRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PageResponse<TableDTO> result = tableService.listTables(pageableRequest, status);
        return ApiResponse.success(result);
    }

    @PostMapping("/create")
    @Operation(summary = "新增桌位", description = "建立新桌位")
    public ApiResponse<TableDTO> createTable(
            @Valid @RequestBody CreateTableRequest request
    ) {
        log.info("新增桌位: {}", request.getTableNumber());
        TableDTO table = tableService.createTable(request);
        return ApiResponse.success("桌位建立成功", table);
    }

    @PostMapping("/update")
    @Operation(summary = "更新桌位", description = "更新桌位資料")
    public ApiResponse<TableDTO> updateTable(
            @Parameter(description = "桌位 ID", required = true, example = "1")
            @RequestParam("id") Long id,
            @Valid @RequestBody UpdateTableRequest request
    ) {
        log.info("更新桌位, id: {}", id);
        TableDTO table = tableService.updateTable(id, request);
        return ApiResponse.success("桌位更新成功", table);
    }

    @PostMapping("/delete")
    @Operation(summary = "刪除桌位", description = "刪除指定桌位 (軟刪除)")
    public ApiResponse<Void> deleteTable(
            @Parameter(description = "桌位 ID", required = true, example = "1")
            @RequestParam("id") Long id
    ) {
        log.info("刪除桌位, id: {}", id);
        tableService.deleteTable(id);
        return ApiResponse.success("桌位刪除成功");
    }
}
