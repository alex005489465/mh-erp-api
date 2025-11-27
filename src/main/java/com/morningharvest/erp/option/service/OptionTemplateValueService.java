package com.morningharvest.erp.option.service;

import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.option.dto.*;
import com.morningharvest.erp.option.entity.OptionTemplateValue;
import com.morningharvest.erp.option.repository.OptionTemplateGroupRepository;
import com.morningharvest.erp.option.repository.OptionTemplateValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptionTemplateValueService {

    private final OptionTemplateValueRepository valueRepository;
    private final OptionTemplateGroupRepository groupRepository;

    @Transactional
    public OptionTemplateValueDTO createValue(CreateOptionTemplateValueRequest request) {
        log.info("建立選項範本值: groupId={}, name={}", request.getGroupId(), request.getName());

        // 驗證群組存在
        if (!groupRepository.existsById(request.getGroupId())) {
            throw new ResourceNotFoundException("選項範本群組不存在: " + request.getGroupId());
        }

        // 驗證同群組內名稱不重複
        if (valueRepository.existsByGroupIdAndName(request.getGroupId(), request.getName())) {
            throw new IllegalArgumentException("群組內選項名稱已存在: " + request.getName());
        }

        OptionTemplateValue value = OptionTemplateValue.builder()
                .groupId(request.getGroupId())
                .name(request.getName())
                .priceAdjustment(request.getPriceAdjustment() != null ? request.getPriceAdjustment() : BigDecimal.ZERO)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(true)
                .build();

        OptionTemplateValue saved = valueRepository.save(value);
        log.info("選項範本值建立成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public OptionTemplateValueDTO updateValue(UpdateOptionTemplateValueRequest request) {
        log.info("更新選項範本值, id: {}", request.getId());

        OptionTemplateValue value = valueRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("選項範本值不存在: " + request.getId()));

        // 驗證同群組內名稱不重複（排除自己）
        if (valueRepository.existsByGroupIdAndNameAndIdNot(value.getGroupId(), request.getName(), request.getId())) {
            throw new IllegalArgumentException("群組內選項名稱已存在: " + request.getName());
        }

        value.setName(request.getName());
        value.setPriceAdjustment(request.getPriceAdjustment());
        if (request.getSortOrder() != null) {
            value.setSortOrder(request.getSortOrder());
        }

        OptionTemplateValue saved = valueRepository.save(value);
        log.info("選項範本值更新成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public void deleteValue(Long id) {
        log.info("刪除選項範本值, id: {}", id);

        if (!valueRepository.existsById(id)) {
            throw new ResourceNotFoundException("選項範本值不存在: " + id);
        }

        valueRepository.deleteById(id);
        log.info("選項範本值刪除成功, id: {}", id);
    }

    @Transactional(readOnly = true)
    public List<OptionTemplateValueDTO> listValuesByGroupId(Long groupId) {
        log.debug("查詢群組下的選項範本值, groupId: {}", groupId);

        // 驗證群組存在
        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("選項範本群組不存在: " + groupId);
        }

        List<OptionTemplateValue> values = valueRepository.findByGroupIdOrderBySortOrder(groupId);

        return values.stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public OptionTemplateValueDTO activateValue(Long id) {
        log.info("啟用選項範本值, id: {}", id);

        OptionTemplateValue value = valueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("選項範本值不存在: " + id));

        value.setIsActive(true);
        OptionTemplateValue saved = valueRepository.save(value);
        log.info("選項範本值啟用成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public OptionTemplateValueDTO deactivateValue(Long id) {
        log.info("停用選項範本值, id: {}", id);

        OptionTemplateValue value = valueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("選項範本值不存在: " + id));

        value.setIsActive(false);
        OptionTemplateValue saved = valueRepository.save(value);
        log.info("選項範本值停用成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    private OptionTemplateValueDTO toDTO(OptionTemplateValue value) {
        return OptionTemplateValueDTO.builder()
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
