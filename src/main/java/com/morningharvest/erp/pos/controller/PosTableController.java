package com.morningharvest.erp.pos.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.pos.dto.EndDiningRequest;
import com.morningharvest.erp.pos.dto.SeatRequest;
import com.morningharvest.erp.pos.dto.TransferTableRequest;
import com.morningharvest.erp.pos.service.PosTableService;
import com.morningharvest.erp.table.dto.TableDTO;
import com.morningharvest.erp.table.dto.TableWithOrderDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pos/tables")
@RequiredArgsConstructor
@Tag(name = "POS 桌位", description = "POS 點餐系統 - 桌位操作")
public class PosTableController {

    private final PosTableService posTableService;

    @GetMapping("/list")
    @Operation(summary = "查詢所有桌位", description = "查詢所有啟用的桌位，包含狀態和當前訂單資訊")
    public ApiResponse<List<TableWithOrderDTO>> listTables() {
        log.debug("POS 查詢所有桌位");
        List<TableWithOrderDTO> tables = posTableService.listTablesWithOrders();
        return ApiResponse.success(tables);
    }

    @GetMapping("/available")
    @Operation(summary = "查詢空桌", description = "查詢所有可用的空桌")
    public ApiResponse<List<TableDTO>> listAvailableTables() {
        log.debug("POS 查詢空桌");
        List<TableDTO> tables = posTableService.listAvailableTables();
        return ApiResponse.success(tables);
    }

    @PostMapping("/seat")
    @Operation(summary = "入座", description = "客人入座。若提供 orderId 則綁定現有訂單，否則建立新的內用訂單")
    public ApiResponse<TableWithOrderDTO> seat(
            @Valid @RequestBody SeatRequest request
    ) {
        log.info("POS 入座, tableId: {}, orderId: {}", request.getTableId(), request.getOrderId());
        TableWithOrderDTO result = posTableService.seat(request);
        return ApiResponse.success("入座成功", result);
    }

    @PostMapping("/transfer")
    @Operation(summary = "換桌", description = "將訂單從來源桌位移至目標桌位")
    public ApiResponse<TableWithOrderDTO> transfer(
            @Valid @RequestBody TransferTableRequest request
    ) {
        log.info("POS 換桌, fromTableId: {}, toTableId: {}", request.getFromTableId(), request.getToTableId());
        TableWithOrderDTO result = posTableService.transfer(request);
        return ApiResponse.success("換桌成功", result);
    }

    @PostMapping("/end")
    @Operation(summary = "結束用餐", description = "結束用餐，清空桌位。需訂單為已付款或已完成狀態")
    public ApiResponse<TableDTO> endDining(
            @Valid @RequestBody EndDiningRequest request
    ) {
        log.info("POS 結束用餐, tableId: {}", request.getTableId());
        TableDTO result = posTableService.endDining(request);
        return ApiResponse.success("結束用餐成功", result);
    }
}
