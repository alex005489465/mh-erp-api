package com.morningharvest.erp.pos.service;

import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.order.dto.OrderDTO;
import com.morningharvest.erp.order.dto.OrderDetailDTO;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.repository.OrderRepository;
import com.morningharvest.erp.pos.dto.EndDiningRequest;
import com.morningharvest.erp.pos.dto.SeatRequest;
import com.morningharvest.erp.pos.dto.TransferTableRequest;
import com.morningharvest.erp.table.constant.TableStatus;
import com.morningharvest.erp.table.dto.TableDTO;
import com.morningharvest.erp.table.dto.TableWithOrderDTO;
import com.morningharvest.erp.table.entity.DiningTable;
import com.morningharvest.erp.table.repository.DiningTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosTableService {

    private final DiningTableRepository diningTableRepository;
    private final OrderRepository orderRepository;

    /**
     * 查詢所有桌位 (含訂單資訊)
     */
    @Transactional(readOnly = true)
    public List<TableWithOrderDTO> listTablesWithOrders() {
        log.debug("查詢所有桌位 (含訂單資訊)");

        List<DiningTable> tables = diningTableRepository.findByIsActiveTrueOrderByTableNumberAsc();

        return tables.stream()
                .map(table -> {
                    OrderDTO orderDTO = null;
                    if (table.getCurrentOrderId() != null) {
                        orderDTO = orderRepository.findById(table.getCurrentOrderId())
                                .map(OrderDTO::from)
                                .orElse(null);
                    }
                    return TableWithOrderDTO.from(table, orderDTO);
                })
                .toList();
    }

    /**
     * 查詢空桌列表
     */
    @Transactional(readOnly = true)
    public List<TableDTO> listAvailableTables() {
        log.debug("查詢空桌列表");

        return diningTableRepository.findByStatusAndIsActiveTrueOrderByTableNumberAsc(TableStatus.AVAILABLE)
                .stream()
                .map(TableDTO::from)
                .toList();
    }

    /**
     * 入座 - 建立/綁定訂單到桌位
     */
    @Transactional
    public TableWithOrderDTO seat(SeatRequest request) {
        log.info("入座, tableId: {}, orderId: {}", request.getTableId(), request.getOrderId());

        // 1. 驗證桌位存在且為空桌
        DiningTable table = diningTableRepository.findById(request.getTableId())
                .orElseThrow(() -> new ResourceNotFoundException("桌位不存在: " + request.getTableId()));

        if (!table.getIsActive()) {
            throw new IllegalArgumentException("桌位已停用");
        }

        if (TableStatus.OCCUPIED.equals(table.getStatus())) {
            throw new IllegalArgumentException("桌位已被佔用: " + table.getTableNumber());
        }

        Order order;

        // 2. 綁定訂單
        if (request.getOrderId() != null) {
            // 綁定既有訂單
            order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("訂單不存在: " + request.getOrderId()));

            if (!"DRAFT".equals(order.getStatus())) {
                throw new IllegalArgumentException("只能綁定草稿狀態的訂單");
            }
        } else {
            // 建立新訂單
            order = Order.builder()
                    .status("DRAFT")
                    .orderType("DINE_IN")
                    .totalAmount(BigDecimal.ZERO)
                    .build();
            order = orderRepository.save(order);
            log.info("建立新訂單, orderId: {}", order.getId());
        }

        // 3. 更新桌位狀態
        table.setStatus(TableStatus.OCCUPIED);
        table.setCurrentOrderId(order.getId());
        diningTableRepository.save(table);

        log.info("入座成功, tableId: {}, tableNumber: {}, orderId: {}",
                table.getId(), table.getTableNumber(), order.getId());

        return TableWithOrderDTO.from(table, OrderDTO.from(order));
    }

    /**
     * 換桌
     */
    @Transactional
    public TableWithOrderDTO transfer(TransferTableRequest request) {
        log.info("換桌, fromTableId: {}, toTableId: {}", request.getFromTableId(), request.getToTableId());

        // 1. 驗證來源桌位 - 必須為佔用狀態
        DiningTable fromTable = diningTableRepository.findById(request.getFromTableId())
                .orElseThrow(() -> new ResourceNotFoundException("來源桌位不存在: " + request.getFromTableId()));

        if (!TableStatus.OCCUPIED.equals(fromTable.getStatus())) {
            throw new IllegalArgumentException("來源桌位非佔用狀態: " + fromTable.getTableNumber());
        }

        if (fromTable.getCurrentOrderId() == null) {
            throw new IllegalArgumentException("來源桌位沒有訂單");
        }

        // 2. 驗證目標桌位 - 必須為空桌
        DiningTable toTable = diningTableRepository.findById(request.getToTableId())
                .orElseThrow(() -> new ResourceNotFoundException("目標桌位不存在: " + request.getToTableId()));

        if (!toTable.getIsActive()) {
            throw new IllegalArgumentException("目標桌位已停用");
        }

        if (TableStatus.OCCUPIED.equals(toTable.getStatus())) {
            throw new IllegalArgumentException("目標桌位已被佔用: " + toTable.getTableNumber());
        }

        // 3. 取得訂單
        Long orderId = fromTable.getCurrentOrderId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在: " + orderId));

        // 4. 清空來源桌位
        fromTable.setStatus(TableStatus.AVAILABLE);
        fromTable.setCurrentOrderId(null);
        diningTableRepository.save(fromTable);

        // 5. 更新目標桌位
        toTable.setStatus(TableStatus.OCCUPIED);
        toTable.setCurrentOrderId(orderId);
        diningTableRepository.save(toTable);

        log.info("換桌成功, fromTable: {}, toTable: {}, orderId: {}",
                fromTable.getTableNumber(), toTable.getTableNumber(), orderId);

        return TableWithOrderDTO.from(toTable, OrderDTO.from(order));
    }

    /**
     * 結束用餐 - 清空桌位
     */
    @Transactional
    public TableDTO endDining(EndDiningRequest request) {
        log.info("結束用餐, tableId: {}", request.getTableId());

        // 1. 驗證桌位存在且為佔用狀態
        DiningTable table = diningTableRepository.findById(request.getTableId())
                .orElseThrow(() -> new ResourceNotFoundException("桌位不存在: " + request.getTableId()));

        if (!TableStatus.OCCUPIED.equals(table.getStatus())) {
            throw new IllegalArgumentException("桌位非佔用狀態: " + table.getTableNumber());
        }

        // 2. 驗證訂單狀態為 PAID 或 COMPLETED
        if (table.getCurrentOrderId() != null) {
            Order order = orderRepository.findById(table.getCurrentOrderId())
                    .orElse(null);

            if (order != null) {
                String status = order.getStatus();
                if (!"PAID".equals(status) && !"COMPLETED".equals(status) && !"CANCELLED".equals(status)) {
                    throw new IllegalArgumentException("訂單尚未結帳或完成，無法結束用餐");
                }
            }
        }

        // 3. 清空桌位
        table.setStatus(TableStatus.AVAILABLE);
        table.setCurrentOrderId(null);
        diningTableRepository.save(table);

        log.info("結束用餐成功, tableId: {}, tableNumber: {}", table.getId(), table.getTableNumber());

        return TableDTO.from(table);
    }
}
