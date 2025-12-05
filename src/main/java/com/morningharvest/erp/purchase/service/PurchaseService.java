package com.morningharvest.erp.purchase.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.event.EventPublisher;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.material.entity.Material;
import com.morningharvest.erp.material.repository.MaterialRepository;
import com.morningharvest.erp.purchase.constant.PurchaseStatus;
import com.morningharvest.erp.purchase.dto.*;
import com.morningharvest.erp.purchase.entity.Purchase;
import com.morningharvest.erp.purchase.entity.PurchaseItem;
import com.morningharvest.erp.purchase.event.PurchaseConfirmedEvent;
import com.morningharvest.erp.purchase.repository.PurchaseItemRepository;
import com.morningharvest.erp.purchase.repository.PurchaseRepository;
import com.morningharvest.erp.supplier.entity.Supplier;
import com.morningharvest.erp.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final EventPublisher eventPublisher;

    /**
     * 建立進貨單
     */
    @Transactional
    public PurchaseDetailDTO createPurchase(CreatePurchaseRequest request) {
        log.info("建立進貨單, supplierId: {}", request.getSupplierId());

        // 驗證供應商
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("供應商不存在: " + request.getSupplierId()));

        if (!supplier.getIsActive()) {
            throw new IllegalArgumentException("供應商已停用: " + supplier.getName());
        }

        // 產生進貨單號
        String purchaseNumber = generatePurchaseNumber();

        // 建立進貨單主檔
        Purchase purchase = Purchase.builder()
                .purchaseNumber(purchaseNumber)
                .supplierId(supplier.getId())
                .supplierName(supplier.getName())
                .status(PurchaseStatus.DRAFT)
                .totalAmount(BigDecimal.ZERO)
                .purchaseDate(request.getPurchaseDate() != null ?
                        request.getPurchaseDate() : LocalDate.now())
                .note(request.getNote())
                .build();

        Purchase saved = purchaseRepository.save(purchase);
        log.info("進貨單建立成功, id: {}, purchaseNumber: {}", saved.getId(), purchaseNumber);

        // 建立明細
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            createPurchaseItems(saved.getId(), request.getItems());
            recalculateTotalAmount(saved);
        }

        return getPurchaseById(saved.getId());
    }

    /**
     * 更新進貨單（只能更新草稿狀態）
     */
    @Transactional
    public PurchaseDetailDTO updatePurchase(UpdatePurchaseRequest request) {
        log.info("更新進貨單, id: {}", request.getId());

        Purchase purchase = findDraftPurchase(request.getId());

        // 驗證供應商
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("供應商不存在: " + request.getSupplierId()));

        if (!supplier.getIsActive()) {
            throw new IllegalArgumentException("供應商已停用: " + supplier.getName());
        }

        // 更新主檔
        purchase.setSupplierId(supplier.getId());
        purchase.setSupplierName(supplier.getName());
        purchase.setPurchaseDate(request.getPurchaseDate());
        purchase.setNote(request.getNote());
        purchaseRepository.save(purchase);

        // 刪除舊明細
        purchaseItemRepository.deleteByPurchaseId(purchase.getId());

        // 建立新明細
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            createPurchaseItems(purchase.getId(), request.getItems());
        }

        // 重新計算總金額
        recalculateTotalAmount(purchase);

        log.info("進貨單更新成功, id: {}", purchase.getId());
        return getPurchaseById(purchase.getId());
    }

    /**
     * 刪除進貨單（只能刪除草稿狀態）
     */
    @Transactional
    public void deletePurchase(Long id) {
        log.info("刪除進貨單, id: {}", id);

        Purchase purchase = findDraftPurchase(id);

        purchaseItemRepository.deleteByPurchaseId(id);
        purchaseRepository.delete(purchase);

        log.info("進貨單刪除成功, id: {}", id);
    }

    /**
     * 確認進貨單（DRAFT -> CONFIRMED）
     */
    @Transactional
    public PurchaseDetailDTO confirmPurchase(ConfirmPurchaseRequest request) {
        log.info("確認進貨單, id: {}", request.getId());

        Purchase purchase = findDraftPurchase(request.getId());

        // 檢查是否有明細
        List<PurchaseItem> items = purchaseItemRepository.findByPurchaseIdOrderByIdAsc(purchase.getId());
        if (items.isEmpty()) {
            throw new IllegalStateException("進貨單沒有明細，無法確認");
        }

        // 更新狀態
        purchase.setStatus(PurchaseStatus.CONFIRMED);
        purchase.setConfirmedAt(LocalDateTime.now());
        purchase.setConfirmedBy(request.getConfirmedBy());
        purchaseRepository.save(purchase);

        log.info("進貨單已確認, id: {}, purchaseNumber: {}", purchase.getId(), purchase.getPurchaseNumber());

        // 發布進貨確認事件
        List<PurchaseConfirmedEvent.PurchaseItemInfo> itemInfos = items.stream()
                .map(item -> new PurchaseConfirmedEvent.PurchaseItemInfo(
                        item.getMaterialId(),
                        item.getMaterialCode(),
                        item.getMaterialName(),
                        item.getQuantity(),
                        item.getUnitPrice()))
                .collect(Collectors.toList());

        eventPublisher.publish(
                new PurchaseConfirmedEvent(
                        purchase.getId(),
                        purchase.getPurchaseNumber(),
                        purchase.getTotalAmount(),
                        itemInfos),
                "進貨確認"
        );

        return getPurchaseById(purchase.getId());
    }

    /**
     * 取得進貨單詳情
     */
    @Transactional(readOnly = true)
    public PurchaseDetailDTO getPurchaseById(Long id) {
        log.debug("查詢進貨單, id: {}", id);

        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("進貨單不存在: " + id));

        List<PurchaseItem> items = purchaseItemRepository.findByPurchaseIdOrderByIdAsc(id);
        List<PurchaseItemDTO> itemDTOs = items.stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());

        return toDetailDTO(purchase, itemDTOs);
    }

    /**
     * 查詢進貨單列表
     */
    @Transactional(readOnly = true)
    public PageResponse<PurchaseDTO> listPurchases(
            PageableRequest pageableRequest,
            String keyword,
            String status,
            Long supplierId,
            LocalDate startDate,
            LocalDate endDate) {

        log.debug("查詢進貨單列表, keyword: {}, status: {}, supplierId: {}, startDate: {}, endDate: {}",
                keyword, status, supplierId, startDate, endDate);

        Page<Purchase> purchasePage = purchaseRepository.findByFilters(
                keyword, status, supplierId, startDate, endDate, pageableRequest.toPageable());

        Page<PurchaseDTO> dtoPage = purchasePage.map(this::toDTO);
        return PageResponse.from(dtoPage);
    }

    // ========== 內部方法 ==========

    /**
     * 產生進貨單號
     * 格式: PO-YYYYMMDD-XXXX
     */
    private String generatePurchaseNumber() {
        String datePrefix = "PO-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        long count = purchaseRepository.countByPurchaseNumberPrefix(datePrefix);
        return datePrefix + String.format("%04d", count + 1);
    }

    /**
     * 取得草稿狀態的進貨單
     */
    private Purchase findDraftPurchase(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("進貨單不存在: " + id));

        if (!PurchaseStatus.DRAFT.equals(purchase.getStatus())) {
            throw new IllegalStateException("只有草稿狀態的進貨單可以操作");
        }

        return purchase;
    }

    /**
     * 建立進貨明細
     */
    private void createPurchaseItems(Long purchaseId, List<CreatePurchaseItemRequest> items) {
        for (CreatePurchaseItemRequest itemRequest : items) {
            // 驗證原物料
            Material material = materialRepository.findById(itemRequest.getMaterialId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "原物料不存在: " + itemRequest.getMaterialId()));

            if (!material.getIsActive()) {
                throw new IllegalArgumentException("原物料已停用: " + material.getName());
            }

            PurchaseItem item = PurchaseItem.builder()
                    .purchaseId(purchaseId)
                    .materialId(material.getId())
                    .materialCode(material.getCode())
                    .materialName(material.getName())
                    .materialUnit(material.getUnit())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .note(itemRequest.getNote())
                    .build();

            item.calculateSubtotal();
            purchaseItemRepository.save(item);
        }
    }

    /**
     * 重新計算總金額
     */
    private void recalculateTotalAmount(Purchase purchase) {
        List<PurchaseItem> items = purchaseItemRepository.findByPurchaseIdOrderByIdAsc(purchase.getId());
        BigDecimal total = items.stream()
                .map(PurchaseItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        purchase.setTotalAmount(total);
        purchaseRepository.save(purchase);
        log.debug("進貨單總金額更新, id: {}, totalAmount: {}", purchase.getId(), total);
    }

    /**
     * Entity 轉 DTO
     */
    private PurchaseDTO toDTO(Purchase purchase) {
        return PurchaseDTO.builder()
                .id(purchase.getId())
                .purchaseNumber(purchase.getPurchaseNumber())
                .supplierId(purchase.getSupplierId())
                .supplierName(purchase.getSupplierName())
                .status(purchase.getStatus())
                .statusDisplayName(PurchaseStatus.getDisplayName(purchase.getStatus()))
                .totalAmount(purchase.getTotalAmount())
                .purchaseDate(purchase.getPurchaseDate())
                .note(purchase.getNote())
                .confirmedAt(purchase.getConfirmedAt())
                .confirmedBy(purchase.getConfirmedBy())
                .createdAt(purchase.getCreatedAt())
                .updatedAt(purchase.getUpdatedAt())
                .build();
    }

    /**
     * Entity 轉 DetailDTO
     */
    private PurchaseDetailDTO toDetailDTO(Purchase purchase, List<PurchaseItemDTO> items) {
        return PurchaseDetailDTO.builder()
                .id(purchase.getId())
                .purchaseNumber(purchase.getPurchaseNumber())
                .supplierId(purchase.getSupplierId())
                .supplierName(purchase.getSupplierName())
                .status(purchase.getStatus())
                .statusDisplayName(PurchaseStatus.getDisplayName(purchase.getStatus()))
                .totalAmount(purchase.getTotalAmount())
                .purchaseDate(purchase.getPurchaseDate())
                .note(purchase.getNote())
                .confirmedAt(purchase.getConfirmedAt())
                .confirmedBy(purchase.getConfirmedBy())
                .createdAt(purchase.getCreatedAt())
                .updatedAt(purchase.getUpdatedAt())
                .items(items)
                .build();
    }

    /**
     * PurchaseItem Entity 轉 DTO
     */
    private PurchaseItemDTO toItemDTO(PurchaseItem item) {
        return PurchaseItemDTO.builder()
                .id(item.getId())
                .purchaseId(item.getPurchaseId())
                .materialId(item.getMaterialId())
                .materialCode(item.getMaterialCode())
                .materialName(item.getMaterialName())
                .materialUnit(item.getMaterialUnit())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .note(item.getNote())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
