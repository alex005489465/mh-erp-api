package com.morningharvest.erp.product.service;

import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.product.dto.*;
import com.morningharvest.erp.product.entity.ProductOptionValue;
import com.morningharvest.erp.product.repository.ProductOptionGroupRepository;
import com.morningharvest.erp.product.repository.ProductOptionValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductOptionValueService {

    private final ProductOptionValueRepository valueRepository;
    private final ProductOptionGroupRepository groupRepository;

    @Transactional
    public ProductOptionValueDTO createValue(CreateProductOptionValueRequest request) {
        log.info("建立產品選項值: groupId={}, name={}", request.getGroupId(), request.getName());

        // 驗證群組存在
        if (!groupRepository.existsById(request.getGroupId())) {
            throw new ResourceNotFoundException("產品選項群組不存在: " + request.getGroupId());
        }

        // 驗證同群組內名稱不重複
        if (valueRepository.existsByGroupIdAndName(request.getGroupId(), request.getName())) {
            throw new IllegalArgumentException("群組內選項名稱已存在: " + request.getName());
        }

        ProductOptionValue value = ProductOptionValue.builder()
                .groupId(request.getGroupId())
                .name(request.getName())
                .priceAdjustment(request.getPriceAdjustment() != null ? request.getPriceAdjustment() : BigDecimal.ZERO)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(true)
                .build();

        ProductOptionValue saved = valueRepository.save(value);
        log.info("產品選項值建立成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public ProductOptionValueDTO updateValue(UpdateProductOptionValueRequest request) {
        log.info("更新產品選項值, id: {}", request.getId());

        ProductOptionValue value = valueRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("產品選項值不存在: " + request.getId()));

        // 驗證同群組內名稱不重複（排除自己）
        if (valueRepository.existsByGroupIdAndNameAndIdNot(value.getGroupId(), request.getName(), request.getId())) {
            throw new IllegalArgumentException("群組內選項名稱已存在: " + request.getName());
        }

        value.setName(request.getName());
        value.setPriceAdjustment(request.getPriceAdjustment());
        if (request.getSortOrder() != null) {
            value.setSortOrder(request.getSortOrder());
        }

        ProductOptionValue saved = valueRepository.save(value);
        log.info("產品選項值更新成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public void deleteValue(Long id) {
        log.info("刪除產品選項值, id: {}", id);

        if (!valueRepository.existsById(id)) {
            throw new ResourceNotFoundException("產品選項值不存在: " + id);
        }

        valueRepository.deleteById(id);
        log.info("產品選項值刪除成功, id: {}", id);
    }

    @Transactional(readOnly = true)
    public List<ProductOptionValueDTO> listValuesByGroupId(Long groupId) {
        log.debug("查詢群組下的產品選項值, groupId: {}", groupId);

        // 驗證群組存在
        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("產品選項群組不存在: " + groupId);
        }

        List<ProductOptionValue> values = valueRepository.findByGroupIdOrderBySortOrder(groupId);

        return values.stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public ProductOptionValueDTO activateValue(Long id) {
        log.info("啟用產品選項值, id: {}", id);

        ProductOptionValue value = valueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("產品選項值不存在: " + id));

        value.setIsActive(true);
        ProductOptionValue saved = valueRepository.save(value);
        log.info("產品選項值啟用成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public ProductOptionValueDTO deactivateValue(Long id) {
        log.info("停用產品選項值, id: {}", id);

        ProductOptionValue value = valueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("產品選項值不存在: " + id));

        value.setIsActive(false);
        ProductOptionValue saved = valueRepository.save(value);
        log.info("產品選項值停用成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    private ProductOptionValueDTO toDTO(ProductOptionValue value) {
        return ProductOptionValueDTO.builder()
                .id(value.getId())
                .groupId(value.getGroupId())
                .name(value.getName())
                .priceAdjustment(value.getPriceAdjustment())
                .sortOrder(value.getSortOrder())
                .isActive(value.getIsActive())
                .createdAt(value.getCreatedAt())
                .updatedAt(value.getUpdatedAt())
                .build();
    }
}
