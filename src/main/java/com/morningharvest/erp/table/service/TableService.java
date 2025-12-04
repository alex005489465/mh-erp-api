package com.morningharvest.erp.table.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.table.constant.TableStatus;
import com.morningharvest.erp.table.dto.CreateTableRequest;
import com.morningharvest.erp.table.dto.TableDTO;
import com.morningharvest.erp.table.dto.UpdateTableRequest;
import com.morningharvest.erp.table.entity.DiningTable;
import com.morningharvest.erp.table.repository.DiningTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TableService {

    private final DiningTableRepository diningTableRepository;

    @Transactional
    public TableDTO createTable(CreateTableRequest request) {
        log.info("建立桌位: {}", request.getTableNumber());

        // 驗證桌號不重複
        if (diningTableRepository.existsByTableNumberAndIsActiveTrue(request.getTableNumber())) {
            throw new IllegalArgumentException("桌號已存在: " + request.getTableNumber());
        }

        DiningTable table = DiningTable.builder()
                .tableNumber(request.getTableNumber())
                .capacity(request.getCapacity() != null ? request.getCapacity() : 4)
                .status(TableStatus.AVAILABLE)
                .isActive(true)
                .note(request.getNote())
                .build();

        DiningTable saved = diningTableRepository.save(table);
        log.info("桌位建立成功, id: {}", saved.getId());

        return TableDTO.from(saved);
    }

    @Transactional
    public TableDTO updateTable(Long id, UpdateTableRequest request) {
        log.info("更新桌位, id: {}", id);

        DiningTable table = diningTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("桌位不存在: " + id));

        // 驗證桌號不重複（排除自己）
        if (request.getTableNumber() != null &&
                diningTableRepository.existsByTableNumberAndIdNotAndIsActiveTrue(request.getTableNumber(), id)) {
            throw new IllegalArgumentException("桌號已存在: " + request.getTableNumber());
        }

        if (request.getTableNumber() != null) {
            table.setTableNumber(request.getTableNumber());
        }
        if (request.getCapacity() != null) {
            table.setCapacity(request.getCapacity());
        }
        if (request.getIsActive() != null) {
            table.setIsActive(request.getIsActive());
        }
        if (request.getNote() != null) {
            table.setNote(request.getNote());
        }

        DiningTable saved = diningTableRepository.save(table);
        log.info("桌位更新成功, id: {}", saved.getId());

        return TableDTO.from(saved);
    }

    @Transactional
    public void deleteTable(Long id) {
        log.info("刪除桌位, id: {}", id);

        DiningTable table = diningTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("桌位不存在: " + id));

        // 檢查桌位是否佔用中
        if (TableStatus.OCCUPIED.equals(table.getStatus())) {
            throw new IllegalArgumentException("無法刪除佔用中的桌位");
        }

        // 軟刪除
        table.setIsActive(false);
        diningTableRepository.save(table);
        log.info("桌位刪除成功, id: {}", id);
    }

    @Transactional(readOnly = true)
    public TableDTO getTableById(Long id) {
        log.debug("查詢桌位, id: {}", id);

        DiningTable table = diningTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("桌位不存在: " + id));

        return TableDTO.from(table);
    }

    @Transactional(readOnly = true)
    public PageResponse<TableDTO> listTables(PageableRequest pageableRequest, String status) {
        log.debug("查詢桌位列表, page: {}, size: {}, status: {}",
                pageableRequest.getPage(), pageableRequest.getSize(), status);

        Page<DiningTable> tablePage;
        if (status != null) {
            tablePage = diningTableRepository.findByStatusAndIsActiveTrue(status, pageableRequest.toPageable());
        } else {
            tablePage = diningTableRepository.findByIsActiveTrue(pageableRequest.toPageable());
        }

        Page<TableDTO> dtoPage = tablePage.map(TableDTO::from);
        return PageResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public List<TableDTO> listAllActiveTables() {
        log.debug("查詢所有啟用桌位");
        return diningTableRepository.findByIsActiveTrueOrderByTableNumberAsc()
                .stream()
                .map(TableDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TableDTO> listAvailableTables() {
        log.debug("查詢所有空桌");
        return diningTableRepository.findByStatusAndIsActiveTrueOrderByTableNumberAsc(TableStatus.AVAILABLE)
                .stream()
                .map(TableDTO::from)
                .toList();
    }
}
