package com.morningharvest.erp.product.service;

import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.product.dto.*;
import com.morningharvest.erp.product.entity.ProductOptionGroup;
import com.morningharvest.erp.product.entity.ProductOptionValue;
import com.morningharvest.erp.product.repository.ProductOptionGroupRepository;
import com.morningharvest.erp.product.repository.ProductOptionValueRepository;
import com.morningharvest.erp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductOptionGroupService {

    private final ProductOptionGroupRepository groupRepository;
    private final ProductOptionValueRepository valueRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ProductOptionGroupDTO createGroup(CreateProductOptionGroupRequest request) {
        log.info("建立產品選項群組: productId={}, name={}", request.getProductId(), request.getName());

        // 驗證產品存在
        if (!productRepository.existsById(request.getProductId())) {
            throw new ResourceNotFoundException("產品不存在: " + request.getProductId());
        }

        // 驗證 minSelections <= maxSelections
        if (request.getMinSelections() > request.getMaxSelections()) {
            throw new IllegalArgumentException("最少選擇數不可大於最多選擇數");
        }

        // 驗證同產品內名稱不重複
        if (groupRepository.existsByProductIdAndName(request.getProductId(), request.getName())) {
            throw new IllegalArgumentException("此產品已有相同名稱的選項群組: " + request.getName());
        }

        ProductOptionGroup group = ProductOptionGroup.builder()
                .productId(request.getProductId())
                .name(request.getName())
                .minSelections(request.getMinSelections())
                .maxSelections(request.getMaxSelections())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(true)
                .build();

        ProductOptionGroup saved = groupRepository.save(group);
        log.info("產品選項群組建立成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public ProductOptionGroupDTO updateGroup(UpdateProductOptionGroupRequest request) {
        log.info("更新產品選項群組, id: {}", request.getId());

        ProductOptionGroup group = groupRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("產品選項群組不存在: " + request.getId()));

        // 驗證 minSelections <= maxSelections
        if (request.getMinSelections() > request.getMaxSelections()) {
            throw new IllegalArgumentException("最少選擇數不可大於最多選擇數");
        }

        // 驗證同產品內名稱不重複（排除自己）
        if (groupRepository.existsByProductIdAndNameAndIdNot(group.getProductId(), request.getName(), request.getId())) {
            throw new IllegalArgumentException("此產品已有相同名稱的選項群組: " + request.getName());
        }

        group.setName(request.getName());
        group.setMinSelections(request.getMinSelections());
        group.setMaxSelections(request.getMaxSelections());
        if (request.getSortOrder() != null) {
            group.setSortOrder(request.getSortOrder());
        }

        ProductOptionGroup saved = groupRepository.save(group);
        log.info("產品選項群組更新成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public void deleteGroup(Long id) {
        log.info("刪除產品選項群組, id: {}", id);

        if (!groupRepository.existsById(id)) {
            throw new ResourceNotFoundException("產品選項群組不存在: " + id);
        }

        // 同時刪除群組下的所有選項值
        valueRepository.deleteByGroupId(id);
        groupRepository.deleteById(id);
        log.info("產品選項群組刪除成功, id: {}", id);
    }

    @Transactional(readOnly = true)
    public ProductOptionGroupDTO getGroupById(Long id) {
        log.debug("查詢產品選項群組, id: {}", id);

        ProductOptionGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("產品選項群組不存在: " + id));

        return toDTO(group);
    }

    @Transactional(readOnly = true)
    public ProductOptionGroupDetailDTO getGroupDetailById(Long id) {
        log.debug("查詢產品選項群組詳情（含選項值）, id: {}", id);

        ProductOptionGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("產品選項群組不存在: " + id));

        List<ProductOptionValue> values = valueRepository.findByGroupIdOrderBySortOrder(id);

        return toDetailDTO(group, values);
    }

    @Transactional(readOnly = true)
    public List<ProductOptionGroupDTO> listGroupsByProductId(Long productId) {
        log.debug("查詢產品選項群組列表, productId: {}", productId);

        // 驗證產品存在
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("產品不存在: " + productId);
        }

        List<ProductOptionGroup> groups = groupRepository.findByProductIdOrderBySortOrder(productId);

        return groups.stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductOptionGroupDetailDTO> listGroupsWithValuesByProductId(Long productId) {
        log.debug("查詢產品選項群組列表（含選項值）, productId: {}", productId);

        // 驗證產品存在
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("產品不存在: " + productId);
        }

        List<ProductOptionGroup> groups = groupRepository.findByProductIdOrderBySortOrder(productId);

        if (groups.isEmpty()) {
            return List.of();
        }

        // 批次載入所有選項值
        List<Long> groupIds = groups.stream().map(ProductOptionGroup::getId).toList();
        List<ProductOptionValue> allValues = valueRepository.findByGroupIdInOrderByGroupIdAndSortOrder(groupIds);

        // 依 groupId 分組
        Map<Long, List<ProductOptionValue>> valuesByGroupId = allValues.stream()
                .collect(Collectors.groupingBy(ProductOptionValue::getGroupId));

        return groups.stream()
                .map(group -> toDetailDTO(group, valuesByGroupId.getOrDefault(group.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ProductOptionGroupDTO activateGroup(Long id) {
        log.info("啟用產品選項群組, id: {}", id);

        ProductOptionGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("產品選項群組不存在: " + id));

        group.setIsActive(true);
        ProductOptionGroup saved = groupRepository.save(group);
        log.info("產品選項群組啟用成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public ProductOptionGroupDTO deactivateGroup(Long id) {
        log.info("停用產品選項群組, id: {}", id);

        ProductOptionGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("產品選項群組不存在: " + id));

        group.setIsActive(false);
        ProductOptionGroup saved = groupRepository.save(group);
        log.info("產品選項群組停用成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    private ProductOptionGroupDTO toDTO(ProductOptionGroup group) {
        return ProductOptionGroupDTO.builder()
                .id(group.getId())
                .productId(group.getProductId())
                .name(group.getName())
                .minSelections(group.getMinSelections())
                .maxSelections(group.getMaxSelections())
                .sortOrder(group.getSortOrder())
                .isActive(group.getIsActive())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    private ProductOptionGroupDetailDTO toDetailDTO(ProductOptionGroup group, List<ProductOptionValue> values) {
        List<ProductOptionValueDTO> valueDTOs = values.stream()
                .map(this::toValueDTO)
                .toList();

        return ProductOptionGroupDetailDTO.builder()
                .id(group.getId())
                .productId(group.getProductId())
                .name(group.getName())
                .minSelections(group.getMinSelections())
                .maxSelections(group.getMaxSelections())
                .sortOrder(group.getSortOrder())
                .isActive(group.getIsActive())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .values(valueDTOs)
                .build();
    }

    private ProductOptionValueDTO toValueDTO(ProductOptionValue value) {
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
