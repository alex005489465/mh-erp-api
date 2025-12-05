package com.morningharvest.erp.material.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.material.constant.MaterialCategory;
import com.morningharvest.erp.material.constant.MaterialUnit;
import com.morningharvest.erp.material.dto.CreateMaterialRequest;
import com.morningharvest.erp.material.dto.MaterialDTO;
import com.morningharvest.erp.material.dto.UpdateMaterialRequest;
import com.morningharvest.erp.material.entity.Material;
import com.morningharvest.erp.material.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialService {

    private final MaterialRepository materialRepository;

    @Transactional
    public MaterialDTO createMaterial(CreateMaterialRequest request) {
        log.info("建立原物料: {}", request.getCode());

        // 驗證編號不重複
        if (materialRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("原物料編號已存在: " + request.getCode());
        }

        // 驗證名稱不重複
        if (materialRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("原物料名稱已存在: " + request.getName());
        }

        // 驗證單位值
        validateUnit(request.getUnit());

        // 驗證分類值（如果有指定）
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            validateCategory(request.getCategory());
        }

        Material material = Material.builder()
                .code(request.getCode())
                .name(request.getName())
                .unit(request.getUnit())
                .category(request.getCategory())
                .specification(request.getSpecification())
                .safeStockQuantity(request.getSafeStockQuantity() != null ?
                        request.getSafeStockQuantity() : BigDecimal.ZERO)
                .currentStockQuantity(request.getCurrentStockQuantity() != null ?
                        request.getCurrentStockQuantity() : BigDecimal.ZERO)
                .costPrice(request.getCostPrice() != null ?
                        request.getCostPrice() : BigDecimal.ZERO)
                .isActive(true)
                .note(request.getNote())
                .build();

        Material saved = materialRepository.save(material);
        log.info("原物料建立成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public MaterialDTO updateMaterial(UpdateMaterialRequest request) {
        log.info("更新原物料, id: {}", request.getId());

        Material material = materialRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("原物料不存在: " + request.getId()));

        // 驗證編號不重複（排除自己）
        if (materialRepository.existsByCodeAndIdNot(request.getCode(), request.getId())) {
            throw new IllegalArgumentException("原物料編號已存在: " + request.getCode());
        }

        // 驗證名稱不重複（排除自己）
        if (materialRepository.existsByNameAndIdNot(request.getName(), request.getId())) {
            throw new IllegalArgumentException("原物料名稱已存在: " + request.getName());
        }

        // 驗證單位值
        validateUnit(request.getUnit());

        // 驗證分類值（如果有指定）
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            validateCategory(request.getCategory());
        }

        material.setCode(request.getCode());
        material.setName(request.getName());
        material.setUnit(request.getUnit());
        material.setCategory(request.getCategory());
        material.setSpecification(request.getSpecification());

        if (request.getSafeStockQuantity() != null) {
            material.setSafeStockQuantity(request.getSafeStockQuantity());
        }
        if (request.getCurrentStockQuantity() != null) {
            material.setCurrentStockQuantity(request.getCurrentStockQuantity());
        }
        if (request.getCostPrice() != null) {
            material.setCostPrice(request.getCostPrice());
        }
        material.setNote(request.getNote());

        Material saved = materialRepository.save(material);
        log.info("原物料更新成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public void deleteMaterial(Long id) {
        log.info("停用原物料, id: {}", id);

        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("原物料不存在: " + id));

        // 軟刪除：設定 is_active = false
        material.setIsActive(false);
        materialRepository.save(material);

        log.info("原物料停用成功, id: {}", id);
    }

    @Transactional(readOnly = true)
    public MaterialDTO getMaterialById(Long id) {
        log.debug("查詢原物料, id: {}", id);

        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("原物料不存在: " + id));

        return toDTO(material);
    }

    @Transactional(readOnly = true)
    public PageResponse<MaterialDTO> listMaterials(
            PageableRequest pageableRequest,
            Boolean isActive,
            String category,
            String keyword) {

        log.debug("查詢原物料列表, page: {}, size: {}, isActive: {}, category: {}, keyword: {}",
                pageableRequest.getPage(), pageableRequest.getSize(), isActive, category, keyword);

        Page<Material> materialPage;

        // 根據不同篩選條件組合查詢
        if (category != null && keyword != null && isActive != null) {
            materialPage = materialRepository.findByCategoryAndNameContainingAndIsActive(
                    category, keyword, isActive, pageableRequest.toPageable());
        } else if (category != null && keyword != null) {
            materialPage = materialRepository.findByCategoryAndNameContaining(
                    category, keyword, pageableRequest.toPageable());
        } else if (category != null && isActive != null) {
            materialPage = materialRepository.findByCategoryAndIsActive(
                    category, isActive, pageableRequest.toPageable());
        } else if (keyword != null && isActive != null) {
            materialPage = materialRepository.findByNameContainingAndIsActive(
                    keyword, isActive, pageableRequest.toPageable());
        } else if (category != null) {
            materialPage = materialRepository.findByCategory(category, pageableRequest.toPageable());
        } else if (keyword != null) {
            materialPage = materialRepository.findByNameContaining(keyword, pageableRequest.toPageable());
        } else if (isActive != null) {
            materialPage = materialRepository.findByIsActive(isActive, pageableRequest.toPageable());
        } else {
            materialPage = materialRepository.findAll(pageableRequest.toPageable());
        }

        Page<MaterialDTO> dtoPage = materialPage.map(this::toDTO);
        return PageResponse.from(dtoPage);
    }

    @Transactional
    public MaterialDTO activateMaterial(Long id) {
        log.info("啟用原物料, id: {}", id);

        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("原物料不存在: " + id));

        material.setIsActive(true);
        Material saved = materialRepository.save(material);
        log.info("原物料啟用成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    /**
     * 驗證單位值是否有效
     */
    private void validateUnit(String unit) {
        if (!MaterialUnit.isValid(unit)) {
            throw new IllegalArgumentException("無效的單位: " + unit +
                    "，有效值為: " + Arrays.toString(MaterialUnit.ALL_UNITS));
        }
    }

    /**
     * 驗證分類值是否有效
     */
    private void validateCategory(String category) {
        if (!MaterialCategory.isValid(category)) {
            throw new IllegalArgumentException("無效的分類: " + category +
                    "，有效值為: " + Arrays.toString(MaterialCategory.ALL_CATEGORIES));
        }
    }

    /**
     * Entity 轉 DTO
     */
    private MaterialDTO toDTO(Material material) {
        return MaterialDTO.builder()
                .id(material.getId())
                .code(material.getCode())
                .name(material.getName())
                .unit(material.getUnit())
                .unitDisplayName(MaterialUnit.getDisplayName(material.getUnit()))
                .category(material.getCategory())
                .categoryDisplayName(MaterialCategory.getDisplayName(material.getCategory()))
                .specification(material.getSpecification())
                .safeStockQuantity(material.getSafeStockQuantity())
                .currentStockQuantity(material.getCurrentStockQuantity())
                .costPrice(material.getCostPrice())
                .isActive(material.getIsActive())
                .note(material.getNote())
                .createdAt(material.getCreatedAt())
                .updatedAt(material.getUpdatedAt())
                .build();
    }
}
