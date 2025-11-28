package com.morningharvest.erp.combo.service;

import com.morningharvest.erp.combo.dto.*;
import com.morningharvest.erp.combo.entity.Combo;
import com.morningharvest.erp.combo.entity.ComboItem;
import com.morningharvest.erp.combo.repository.ComboItemRepository;
import com.morningharvest.erp.combo.repository.ComboRepository;
import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.product.dto.ProductOptionGroupDetailDTO;
import com.morningharvest.erp.product.entity.ProductCategory;
import com.morningharvest.erp.product.repository.ProductCategoryRepository;
import com.morningharvest.erp.product.service.ProductOptionGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComboService {

    private final ComboRepository comboRepository;
    private final ComboItemRepository comboItemRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductOptionGroupService productOptionGroupService;

    @Transactional
    public ComboDTO createCombo(CreateComboRequest request) {
        log.info("建立套餐: {}", request.getName());

        // 驗證名稱不重複
        if (comboRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("套餐名稱已存在: " + request.getName());
        }

        // 查詢分類資訊（如果有指定）
        String categoryName = null;
        if (request.getCategoryId() != null) {
            ProductCategory category = productCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("商品分類不存在: " + request.getCategoryId()));
            categoryName = category.getName();
        }

        Combo combo = Combo.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .categoryId(request.getCategoryId())
                .categoryName(categoryName)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(true)
                .build();

        Combo saved = comboRepository.save(combo);
        log.info("套餐建立成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public ComboDTO updateCombo(Long id, UpdateComboRequest request) {
        log.info("更新套餐, id: {}", id);

        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("套餐不存在: " + id));

        // 驗證名稱不重複（排除自己）
        if (comboRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new IllegalArgumentException("套餐名稱已存在: " + request.getName());
        }

        // 查詢分類資訊（如果有指定）
        String categoryName = null;
        if (request.getCategoryId() != null) {
            ProductCategory category = productCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("商品分類不存在: " + request.getCategoryId()));
            categoryName = category.getName();
        }

        combo.setName(request.getName());
        combo.setDescription(request.getDescription());
        combo.setPrice(request.getPrice());
        combo.setImageUrl(request.getImageUrl());
        combo.setCategoryId(request.getCategoryId());
        combo.setCategoryName(categoryName);
        if (request.getIsActive() != null) {
            combo.setIsActive(request.getIsActive());
        }
        if (request.getSortOrder() != null) {
            combo.setSortOrder(request.getSortOrder());
        }

        Combo saved = comboRepository.save(combo);
        log.info("套餐更新成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public void deleteCombo(Long id) {
        log.info("刪除套餐, id: {}", id);

        if (!comboRepository.existsById(id)) {
            throw new ResourceNotFoundException("套餐不存在: " + id);
        }

        // 同時刪除套餐項目
        comboItemRepository.deleteByComboId(id);
        comboRepository.deleteById(id);
        log.info("套餐刪除成功, id: {}", id);
    }

    @Transactional(readOnly = true)
    public ComboDTO getComboById(Long id) {
        log.debug("查詢套餐, id: {}", id);

        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("套餐不存在: " + id));

        return toDTO(combo);
    }

    @Transactional(readOnly = true)
    public ComboDetailDTO getComboDetailById(Long id) {
        log.debug("查詢套餐詳情（含項目與選項）, id: {}", id);

        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("套餐不存在: " + id));

        List<ComboItem> items = comboItemRepository.findByComboIdOrderBySortOrder(id);

        // 批次載入所有商品的選項群組
        List<Long> productIds = items.stream()
                .map(ComboItem::getProductId)
                .distinct()
                .toList();

        Map<Long, List<ProductOptionGroupDetailDTO>> optionsByProductId = productIds.stream()
                .collect(Collectors.toMap(
                        productId -> productId,
                        productOptionGroupService::listGroupsWithValuesByProductId
                ));

        List<ComboItemDetailDTO> itemDetails = items.stream()
                .map(item -> ComboItemDetailDTO.builder()
                        .id(item.getId())
                        .comboId(item.getComboId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .sortOrder(item.getSortOrder())
                        .createdAt(item.getCreatedAt())
                        .updatedAt(item.getUpdatedAt())
                        .optionGroups(optionsByProductId.getOrDefault(item.getProductId(), List.of()))
                        .build())
                .toList();

        return ComboDetailDTO.builder()
                .id(combo.getId())
                .name(combo.getName())
                .description(combo.getDescription())
                .price(combo.getPrice())
                .imageUrl(combo.getImageUrl())
                .categoryId(combo.getCategoryId())
                .categoryName(combo.getCategoryName())
                .isActive(combo.getIsActive())
                .sortOrder(combo.getSortOrder())
                .createdAt(combo.getCreatedAt())
                .updatedAt(combo.getUpdatedAt())
                .items(itemDetails)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<ComboDTO> listCombos(PageableRequest pageableRequest, Boolean isActive, Long categoryId) {
        log.debug("查詢套餐列表, page: {}, size: {}, isActive: {}, categoryId: {}",
                pageableRequest.getPage(), pageableRequest.getSize(), isActive, categoryId);

        Page<Combo> comboPage;

        if (categoryId != null && isActive != null) {
            comboPage = comboRepository.findByCategoryIdAndIsActive(categoryId, isActive, pageableRequest.toPageable());
        } else if (categoryId != null) {
            comboPage = comboRepository.findByCategoryId(categoryId, pageableRequest.toPageable());
        } else if (isActive != null) {
            comboPage = comboRepository.findByIsActive(isActive, pageableRequest.toPageable());
        } else {
            comboPage = comboRepository.findAll(pageableRequest.toPageable());
        }

        Page<ComboDTO> dtoPage = comboPage.map(this::toDTO);
        return PageResponse.from(dtoPage);
    }

    @Transactional
    public ComboDTO activateCombo(Long id) {
        log.info("啟用套餐, id: {}", id);

        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("套餐不存在: " + id));

        combo.setIsActive(true);
        Combo saved = comboRepository.save(combo);
        log.info("套餐啟用成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public ComboDTO deactivateCombo(Long id) {
        log.info("停用套餐, id: {}", id);

        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("套餐不存在: " + id));

        combo.setIsActive(false);
        Combo saved = comboRepository.save(combo);
        log.info("套餐停用成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    private ComboDTO toDTO(Combo combo) {
        return ComboDTO.builder()
                .id(combo.getId())
                .name(combo.getName())
                .description(combo.getDescription())
                .price(combo.getPrice())
                .imageUrl(combo.getImageUrl())
                .categoryId(combo.getCategoryId())
                .categoryName(combo.getCategoryName())
                .isActive(combo.getIsActive())
                .sortOrder(combo.getSortOrder())
                .createdAt(combo.getCreatedAt())
                .updatedAt(combo.getUpdatedAt())
                .build();
    }
}
