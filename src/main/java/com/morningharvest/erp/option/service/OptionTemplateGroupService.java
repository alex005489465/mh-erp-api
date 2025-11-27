package com.morningharvest.erp.option.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.option.dto.*;
import com.morningharvest.erp.option.entity.OptionTemplateGroup;
import com.morningharvest.erp.option.entity.OptionTemplateValue;
import com.morningharvest.erp.option.repository.OptionTemplateGroupRepository;
import com.morningharvest.erp.option.repository.OptionTemplateValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptionTemplateGroupService {

    private final OptionTemplateGroupRepository groupRepository;
    private final OptionTemplateValueRepository valueRepository;

    @Transactional
    public OptionTemplateGroupDTO createGroup(CreateOptionTemplateGroupRequest request) {
        log.info("建立選項範本群組: {}", request.getName());

        // 驗證 minSelections <= maxSelections
        if (request.getMinSelections() > request.getMaxSelections()) {
            throw new IllegalArgumentException("最少選擇數不可大於最多選擇數");
        }

        // 驗證名稱不重複
        if (groupRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("群組名稱已存在: " + request.getName());
        }

        OptionTemplateGroup group = OptionTemplateGroup.builder()
                .name(request.getName())
                .minSelections(request.getMinSelections())
                .maxSelections(request.getMaxSelections())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(true)
                .build();

        OptionTemplateGroup saved = groupRepository.save(group);
        log.info("選項範本群組建立成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public OptionTemplateGroupDTO updateGroup(UpdateOptionTemplateGroupRequest request) {
        log.info("更新選項範本群組, id: {}", request.getId());

        OptionTemplateGroup group = groupRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("選項範本群組不存在: " + request.getId()));

        // 驗證 minSelections <= maxSelections
        if (request.getMinSelections() > request.getMaxSelections()) {
            throw new IllegalArgumentException("最少選擇數不可大於最多選擇數");
        }

        // 驗證名稱不重複（排除自己）
        if (groupRepository.existsByNameAndIdNot(request.getName(), request.getId())) {
            throw new IllegalArgumentException("群組名稱已存在: " + request.getName());
        }

        group.setName(request.getName());
        group.setMinSelections(request.getMinSelections());
        group.setMaxSelections(request.getMaxSelections());
        if (request.getSortOrder() != null) {
            group.setSortOrder(request.getSortOrder());
        }

        OptionTemplateGroup saved = groupRepository.save(group);
        log.info("選項範本群組更新成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public void deleteGroup(Long id) {
        log.info("刪除選項範本群組, id: {}", id);

        if (!groupRepository.existsById(id)) {
            throw new ResourceNotFoundException("選項範本群組不存在: " + id);
        }

        // 同時刪除群組下的所有選項值
        valueRepository.deleteByGroupId(id);
        groupRepository.deleteById(id);
        log.info("選項範本群組刪除成功, id: {}", id);
    }

    @Transactional(readOnly = true)
    public OptionTemplateGroupDTO getGroupById(Long id) {
        log.debug("查詢選項範本群組, id: {}", id);

        OptionTemplateGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("選項範本群組不存在: " + id));

        return toDTO(group);
    }

    @Transactional(readOnly = true)
    public OptionTemplateGroupDetailDTO getGroupDetailById(Long id) {
        log.debug("查詢選項範本群組詳情（含選項值）, id: {}", id);

        OptionTemplateGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("選項範本群組不存在: " + id));

        List<OptionTemplateValue> values = valueRepository.findByGroupIdOrderBySortOrder(id);

        return toDetailDTO(group, values);
    }

    @Transactional(readOnly = true)
    public PageResponse<OptionTemplateGroupDTO> listGroups(PageableRequest pageableRequest, Boolean isActive) {
        log.debug("查詢選項範本群組列表, page: {}, size: {}, isActive: {}",
                pageableRequest.getPage(), pageableRequest.getSize(), isActive);

        Page<OptionTemplateGroup> groupPage;
        if (isActive != null) {
            groupPage = groupRepository.findByIsActive(isActive, pageableRequest.toPageable());
        } else {
            groupPage = groupRepository.findAll(pageableRequest.toPageable());
        }

        Page<OptionTemplateGroupDTO> dtoPage = groupPage.map(this::toDTO);
        return PageResponse.from(dtoPage);
    }

    @Transactional
    public OptionTemplateGroupDTO activateGroup(Long id) {
        log.info("啟用選項範本群組, id: {}", id);

        OptionTemplateGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("選項範本群組不存在: " + id));

        group.setIsActive(true);
        OptionTemplateGroup saved = groupRepository.save(group);
        log.info("選項範本群組啟用成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public OptionTemplateGroupDTO deactivateGroup(Long id) {
        log.info("停用選項範本群組, id: {}", id);

        OptionTemplateGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("選項範本群組不存在: " + id));

        group.setIsActive(false);
        OptionTemplateGroup saved = groupRepository.save(group);
        log.info("選項範本群組停用成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    private OptionTemplateGroupDTO toDTO(OptionTemplateGroup group) {
        return OptionTemplateGroupDTO.builder()
                .id(group.getId())
                .name(group.getName())
                .minSelections(group.getMinSelections())
                .maxSelections(group.getMaxSelections())
                .sortOrder(group.getSortOrder())
                .isActive(group.getIsActive())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    private OptionTemplateGroupDetailDTO toDetailDTO(OptionTemplateGroup group, List<OptionTemplateValue> values) {
        List<OptionTemplateValueDTO> valueDTOs = values.stream()
                .map(this::toValueDTO)
                .toList();

        return OptionTemplateGroupDetailDTO.builder()
                .id(group.getId())
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

    private OptionTemplateValueDTO toValueDTO(OptionTemplateValue value) {
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
