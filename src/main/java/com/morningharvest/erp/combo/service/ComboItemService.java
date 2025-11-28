package com.morningharvest.erp.combo.service;

import com.morningharvest.erp.combo.dto.*;
import com.morningharvest.erp.combo.entity.ComboItem;
import com.morningharvest.erp.combo.repository.ComboItemRepository;
import com.morningharvest.erp.combo.repository.ComboRepository;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComboItemService {

    private final ComboItemRepository comboItemRepository;
    private final ComboRepository comboRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ComboItemDTO createComboItem(CreateComboItemRequest request) {
        log.info("建立套餐項目: comboId={}, productId={}", request.getComboId(), request.getProductId());

        // 驗證套餐存在
        if (!comboRepository.existsById(request.getComboId())) {
            throw new ResourceNotFoundException("套餐不存在: " + request.getComboId());
        }

        // 驗證商品存在並取得名稱
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在: " + request.getProductId()));

        // 驗證同一套餐內不重複新增相同商品
        if (comboItemRepository.existsByComboIdAndProductId(request.getComboId(), request.getProductId())) {
            throw new IllegalArgumentException("此套餐已包含此商品，請更新數量而非重複新增");
        }

        ComboItem item = ComboItem.builder()
                .comboId(request.getComboId())
                .productId(request.getProductId())
                .productName(product.getName())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        ComboItem saved = comboItemRepository.save(item);
        log.info("套餐項目建立成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public List<ComboItemDTO> batchCreateComboItems(BatchCreateComboItemRequest request) {
        log.info("批次建立套餐項目: comboId={}, itemCount={}", request.getComboId(), request.getItems().size());

        // 驗證套餐存在
        if (!comboRepository.existsById(request.getComboId())) {
            throw new ResourceNotFoundException("套餐不存在: " + request.getComboId());
        }

        List<ComboItem> itemsToSave = new ArrayList<>();

        for (BatchCreateComboItemRequest.ComboItemInput input : request.getItems()) {
            // 驗證商品存在並取得名稱
            Product product = productRepository.findById(input.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("商品不存在: " + input.getProductId()));

            // 檢查是否已存在
            if (comboItemRepository.existsByComboIdAndProductId(request.getComboId(), input.getProductId())) {
                throw new IllegalArgumentException("此套餐已包含商品: " + product.getName());
            }

            ComboItem item = ComboItem.builder()
                    .comboId(request.getComboId())
                    .productId(input.getProductId())
                    .productName(product.getName())
                    .quantity(input.getQuantity() != null ? input.getQuantity() : 1)
                    .sortOrder(input.getSortOrder() != null ? input.getSortOrder() : 0)
                    .build();

            itemsToSave.add(item);
        }

        List<ComboItem> saved = comboItemRepository.saveAll(itemsToSave);
        log.info("批次建立套餐項目成功, 共 {} 筆", saved.size());

        return saved.stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public ComboItemDTO updateComboItem(Long id, UpdateComboItemRequest request) {
        log.info("更新套餐項目, id: {}", id);

        ComboItem item = comboItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("套餐項目不存在: " + id));

        // 如果商品 ID 有變更，需要驗證
        if (request.getProductId() != null && !request.getProductId().equals(item.getProductId())) {
            // 驗證新商品存在
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("商品不存在: " + request.getProductId()));

            // 檢查是否已存在（排除自己）
            if (comboItemRepository.existsByComboIdAndProductIdAndIdNot(item.getComboId(), request.getProductId(), id)) {
                throw new IllegalArgumentException("此套餐已包含此商品");
            }

            item.setProductId(request.getProductId());
            item.setProductName(product.getName());
        }

        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        if (request.getSortOrder() != null) {
            item.setSortOrder(request.getSortOrder());
        }

        ComboItem saved = comboItemRepository.save(item);
        log.info("套餐項目更新成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public void deleteComboItem(Long id) {
        log.info("刪除套餐項目, id: {}", id);

        if (!comboItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("套餐項目不存在: " + id);
        }

        comboItemRepository.deleteById(id);
        log.info("套餐項目刪除成功, id: {}", id);
    }

    @Transactional(readOnly = true)
    public ComboItemDTO getComboItemById(Long id) {
        log.debug("查詢套餐項目, id: {}", id);

        ComboItem item = comboItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("套餐項目不存在: " + id));

        return toDTO(item);
    }

    @Transactional(readOnly = true)
    public List<ComboItemDTO> listComboItemsByComboId(Long comboId) {
        log.debug("查詢套餐項目列表, comboId: {}", comboId);

        // 驗證套餐存在
        if (!comboRepository.existsById(comboId)) {
            throw new ResourceNotFoundException("套餐不存在: " + comboId);
        }

        List<ComboItem> items = comboItemRepository.findByComboIdOrderBySortOrder(comboId);

        return items.stream()
                .map(this::toDTO)
                .toList();
    }

    private ComboItemDTO toDTO(ComboItem item) {
        return ComboItemDTO.builder()
                .id(item.getId())
                .comboId(item.getComboId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .sortOrder(item.getSortOrder())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
