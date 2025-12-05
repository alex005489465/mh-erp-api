package com.morningharvest.erp.inventorycheck.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.event.EventPublisher;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.inventorycheck.constant.InventoryCheckStatus;
import com.morningharvest.erp.inventorycheck.dto.*;
import com.morningharvest.erp.inventorycheck.entity.InventoryCheck;
import com.morningharvest.erp.inventorycheck.entity.InventoryCheckItem;
import com.morningharvest.erp.inventorycheck.event.InventoryCheckConfirmedEvent;
import com.morningharvest.erp.inventorycheck.repository.InventoryCheckItemRepository;
import com.morningharvest.erp.inventorycheck.repository.InventoryCheckRepository;
import com.morningharvest.erp.material.entity.Material;
import com.morningharvest.erp.material.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryCheckService {

    private final InventoryCheckRepository inventoryCheckRepository;
    private final InventoryCheckItemRepository inventoryCheckItemRepository;
    private final MaterialRepository materialRepository;
    private final EventPublisher eventPublisher;

    /**
     * 建立盤點計畫
     * 自動載入所有啟用中的原物料
     */
    @Transactional
    public InventoryCheckDetailDTO createInventoryCheck(CreateInventoryCheckRequest request) {
        log.info("建立盤點計畫");

        // 產生盤點單號
        String checkNumber = generateCheckNumber();

        // 建立盤點單主檔
        InventoryCheck inventoryCheck = InventoryCheck.builder()
                .checkNumber(checkNumber)
                .status(InventoryCheckStatus.PLANNED)
                .checkDate(request.getCheckDate() != null ? request.getCheckDate() : LocalDate.now())
                .totalItems(0)
                .totalDifferenceAmount(BigDecimal.ZERO)
                .note(request.getNote())
                .build();

        InventoryCheck saved = inventoryCheckRepository.save(inventoryCheck);
        log.info("盤點單已建立, id: {}, checkNumber: {}", saved.getId(), checkNumber);

        // 載入所有啟用中的原物料作為盤點項目
        List<Material> activeMaterials = materialRepository.findByIsActive(true, Pageable.unpaged()).getContent();

        for (Material material : activeMaterials) {
            InventoryCheckItem item = InventoryCheckItem.builder()
                    .inventoryCheckId(saved.getId())
                    .materialId(material.getId())
                    .materialCode(material.getCode())
                    .materialName(material.getName())
                    .materialUnit(material.getUnit())
                    .systemQuantity(material.getCurrentStockQuantity() != null ?
                            material.getCurrentStockQuantity() : BigDecimal.ZERO)
                    .unitCost(material.getCostPrice() != null ?
                            material.getCostPrice() : BigDecimal.ZERO)
                    .isChecked(false)
                    .build();
            inventoryCheckItemRepository.save(item);
        }

        // 更新品項總數
        saved.setTotalItems(activeMaterials.size());
        inventoryCheckRepository.save(saved);

        log.info("盤點計畫已建立，共 {} 個品項", activeMaterials.size());
        return getInventoryCheckById(saved.getId());
    }

    /**
     * 開始盤點 (PLANNED -> IN_PROGRESS)
     */
    @Transactional
    public InventoryCheckDetailDTO startInventoryCheck(StartInventoryCheckRequest request) {
        log.info("開始盤點, id: {}", request.getId());

        InventoryCheck inventoryCheck = findPlannedInventoryCheck(request.getId());

        // 檢查是否有盤點項目
        if (!inventoryCheckItemRepository.existsByInventoryCheckId(inventoryCheck.getId())) {
            throw new IllegalStateException("盤點單沒有項目，無法開始盤點");
        }

        // 更新狀態
        inventoryCheck.setStatus(InventoryCheckStatus.IN_PROGRESS);
        inventoryCheck.setStartedAt(LocalDateTime.now());
        inventoryCheck.setStartedBy(request.getStartedBy());
        inventoryCheckRepository.save(inventoryCheck);

        log.info("盤點已開始, id: {}, checkNumber: {}",
                inventoryCheck.getId(), inventoryCheck.getCheckNumber());

        return getInventoryCheckById(inventoryCheck.getId());
    }

    /**
     * 更新盤點明細（輸入實際數量）
     */
    @Transactional
    public InventoryCheckItemDTO updateInventoryCheckItem(UpdateInventoryCheckItemRequest request) {
        log.info("更新盤點明細, itemId: {}", request.getItemId());

        InventoryCheckItem item = inventoryCheckItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "盤點明細不存在: " + request.getItemId()));

        // 驗證盤點單狀態為盤點中
        InventoryCheck inventoryCheck = inventoryCheckRepository.findById(item.getInventoryCheckId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "盤點單不存在: " + item.getInventoryCheckId()));

        if (!InventoryCheckStatus.IN_PROGRESS.equals(inventoryCheck.getStatus())) {
            throw new IllegalStateException("只有盤點中狀態才能更新盤點數量");
        }

        // 更新明細
        item.setActualQuantity(request.getActualQuantity());
        item.setNote(request.getNote());
        item.setIsChecked(true);
        item.calculateDifference();
        inventoryCheckItemRepository.save(item);

        // 重新計算總盤差金額
        recalculateTotalDifferenceAmount(inventoryCheck);

        log.info("盤點明細已更新, itemId: {}, actualQuantity: {}, differenceQuantity: {}",
                item.getId(), item.getActualQuantity(), item.getDifferenceQuantity());

        return toItemDTO(item);
    }

    /**
     * 確認盤點 (IN_PROGRESS -> CONFIRMED)
     */
    @Transactional
    public InventoryCheckDetailDTO confirmInventoryCheck(ConfirmInventoryCheckRequest request) {
        log.info("確認盤點, id: {}", request.getId());

        InventoryCheck inventoryCheck = findInProgressInventoryCheck(request.getId());

        // 檢查是否所有項目都已盤點
        long uncheckedCount = inventoryCheckItemRepository
                .countByInventoryCheckIdAndIsCheckedFalse(inventoryCheck.getId());
        if (uncheckedCount > 0) {
            throw new IllegalStateException(
                    "無法確認：尚有 " + uncheckedCount + " 個品項未盤點");
        }

        // 更新狀態
        inventoryCheck.setStatus(InventoryCheckStatus.CONFIRMED);
        inventoryCheck.setConfirmedAt(LocalDateTime.now());
        inventoryCheck.setConfirmedBy(request.getConfirmedBy());
        inventoryCheckRepository.save(inventoryCheck);

        log.info("盤點已確認, id: {}, checkNumber: {}",
                inventoryCheck.getId(), inventoryCheck.getCheckNumber());

        // 發布盤點確認事件
        List<InventoryCheckItem> items = inventoryCheckItemRepository
                .findByInventoryCheckIdOrderByIdAsc(inventoryCheck.getId());

        List<InventoryCheckConfirmedEvent.InventoryCheckItemInfo> itemInfos = items.stream()
                .map(item -> new InventoryCheckConfirmedEvent.InventoryCheckItemInfo(
                        item.getMaterialId(),
                        item.getMaterialCode(),
                        item.getMaterialName(),
                        item.getSystemQuantity(),
                        item.getActualQuantity(),
                        item.getDifferenceQuantity()))
                .collect(Collectors.toList());

        eventPublisher.publish(
                new InventoryCheckConfirmedEvent(
                        inventoryCheck.getId(),
                        inventoryCheck.getCheckNumber(),
                        inventoryCheck.getTotalDifferenceAmount(),
                        itemInfos),
                "盤點確認"
        );

        return getInventoryCheckById(inventoryCheck.getId());
    }

    /**
     * 刪除盤點單（僅 PLANNED 狀態）
     */
    @Transactional
    public void deleteInventoryCheck(Long id) {
        log.info("刪除盤點單, id: {}", id);

        InventoryCheck inventoryCheck = findPlannedInventoryCheck(id);

        inventoryCheckItemRepository.deleteByInventoryCheckId(id);
        inventoryCheckRepository.delete(inventoryCheck);

        log.info("盤點單已刪除, id: {}", id);
    }

    /**
     * 取得盤點單詳情
     */
    @Transactional(readOnly = true)
    public InventoryCheckDetailDTO getInventoryCheckById(Long id) {
        log.debug("查詢盤點單, id: {}", id);

        InventoryCheck inventoryCheck = inventoryCheckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("盤點單不存在: " + id));

        List<InventoryCheckItem> items = inventoryCheckItemRepository
                .findByInventoryCheckIdOrderByIdAsc(id);
        List<InventoryCheckItemDTO> itemDTOs = items.stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());

        long checkedCount = inventoryCheckItemRepository
                .countByInventoryCheckIdAndIsCheckedTrue(id);
        long uncheckedCount = inventoryCheckItemRepository
                .countByInventoryCheckIdAndIsCheckedFalse(id);

        return toDetailDTO(inventoryCheck, itemDTOs, (int) checkedCount, (int) uncheckedCount);
    }

    /**
     * 分頁查詢盤點單列表
     */
    @Transactional(readOnly = true)
    public PageResponse<InventoryCheckDTO> listInventoryChecks(
            PageableRequest pageableRequest,
            String keyword,
            String status,
            LocalDate startDate,
            LocalDate endDate) {

        log.debug("查詢盤點單列表, keyword: {}, status: {}, startDate: {}, endDate: {}",
                keyword, status, startDate, endDate);

        Page<InventoryCheck> page = inventoryCheckRepository.findByFilters(
                keyword, status, startDate, endDate, pageableRequest.toPageable());

        Page<InventoryCheckDTO> dtoPage = page.map(this::toDTO);
        return PageResponse.from(dtoPage);
    }

    // ========== 內部方法 ==========

    /**
     * 產生盤點單號
     * 格式: IC-YYYYMMDD-XXXX
     */
    private String generateCheckNumber() {
        String datePrefix = "IC-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        long count = inventoryCheckRepository.countByCheckNumberPrefix(datePrefix);
        return datePrefix + String.format("%04d", count + 1);
    }

    /**
     * 查詢 PLANNED 狀態的盤點單
     */
    private InventoryCheck findPlannedInventoryCheck(Long id) {
        InventoryCheck inventoryCheck = inventoryCheckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("盤點單不存在: " + id));

        if (!InventoryCheckStatus.PLANNED.equals(inventoryCheck.getStatus())) {
            throw new IllegalStateException("只有計畫中狀態的盤點單才能進行此操作");
        }

        return inventoryCheck;
    }

    /**
     * 查詢 IN_PROGRESS 狀態的盤點單
     */
    private InventoryCheck findInProgressInventoryCheck(Long id) {
        InventoryCheck inventoryCheck = inventoryCheckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("盤點單不存在: " + id));

        if (!InventoryCheckStatus.IN_PROGRESS.equals(inventoryCheck.getStatus())) {
            throw new IllegalStateException("只有盤點中狀態的盤點單才能確認");
        }

        return inventoryCheck;
    }

    /**
     * 重新計算總盤差金額
     */
    private void recalculateTotalDifferenceAmount(InventoryCheck inventoryCheck) {
        List<InventoryCheckItem> items = inventoryCheckItemRepository
                .findByInventoryCheckIdOrderByIdAsc(inventoryCheck.getId());

        BigDecimal total = items.stream()
                .filter(item -> item.getDifferenceAmount() != null)
                .map(InventoryCheckItem::getDifferenceAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        inventoryCheck.setTotalDifferenceAmount(total);
        inventoryCheckRepository.save(inventoryCheck);
        log.debug("總盤差金額已更新, id: {}, amount: {}",
                inventoryCheck.getId(), total);
    }

    /**
     * Entity 轉 DTO
     */
    private InventoryCheckDTO toDTO(InventoryCheck inventoryCheck) {
        return InventoryCheckDTO.builder()
                .id(inventoryCheck.getId())
                .checkNumber(inventoryCheck.getCheckNumber())
                .status(inventoryCheck.getStatus())
                .statusDisplayName(InventoryCheckStatus.getDisplayName(inventoryCheck.getStatus()))
                .checkDate(inventoryCheck.getCheckDate())
                .totalItems(inventoryCheck.getTotalItems())
                .totalDifferenceAmount(inventoryCheck.getTotalDifferenceAmount())
                .note(inventoryCheck.getNote())
                .startedAt(inventoryCheck.getStartedAt())
                .startedBy(inventoryCheck.getStartedBy())
                .confirmedAt(inventoryCheck.getConfirmedAt())
                .confirmedBy(inventoryCheck.getConfirmedBy())
                .createdAt(inventoryCheck.getCreatedAt())
                .updatedAt(inventoryCheck.getUpdatedAt())
                .build();
    }

    /**
     * Entity 轉 DetailDTO
     */
    private InventoryCheckDetailDTO toDetailDTO(InventoryCheck inventoryCheck,
                                                 List<InventoryCheckItemDTO> items,
                                                 int checkedItems, int uncheckedItems) {
        return InventoryCheckDetailDTO.builder()
                .id(inventoryCheck.getId())
                .checkNumber(inventoryCheck.getCheckNumber())
                .status(inventoryCheck.getStatus())
                .statusDisplayName(InventoryCheckStatus.getDisplayName(inventoryCheck.getStatus()))
                .checkDate(inventoryCheck.getCheckDate())
                .totalItems(inventoryCheck.getTotalItems())
                .checkedItems(checkedItems)
                .uncheckedItems(uncheckedItems)
                .totalDifferenceAmount(inventoryCheck.getTotalDifferenceAmount())
                .note(inventoryCheck.getNote())
                .startedAt(inventoryCheck.getStartedAt())
                .startedBy(inventoryCheck.getStartedBy())
                .confirmedAt(inventoryCheck.getConfirmedAt())
                .confirmedBy(inventoryCheck.getConfirmedBy())
                .createdAt(inventoryCheck.getCreatedAt())
                .updatedAt(inventoryCheck.getUpdatedAt())
                .items(items)
                .build();
    }

    /**
     * InventoryCheckItem Entity 轉 DTO
     */
    private InventoryCheckItemDTO toItemDTO(InventoryCheckItem item) {
        return InventoryCheckItemDTO.builder()
                .id(item.getId())
                .inventoryCheckId(item.getInventoryCheckId())
                .materialId(item.getMaterialId())
                .materialCode(item.getMaterialCode())
                .materialName(item.getMaterialName())
                .materialUnit(item.getMaterialUnit())
                .systemQuantity(item.getSystemQuantity())
                .actualQuantity(item.getActualQuantity())
                .differenceQuantity(item.getDifferenceQuantity())
                .unitCost(item.getUnitCost())
                .differenceAmount(item.getDifferenceAmount())
                .isChecked(item.getIsChecked())
                .note(item.getNote())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
