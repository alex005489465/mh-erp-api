package com.morningharvest.erp.option.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.option.dto.*;
import com.morningharvest.erp.option.entity.OptionTemplateGroup;
import com.morningharvest.erp.option.entity.OptionTemplateValue;
import com.morningharvest.erp.option.repository.OptionTemplateGroupRepository;
import com.morningharvest.erp.option.repository.OptionTemplateValueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OptionTemplateGroupService 單元測試")
class OptionTemplateGroupServiceTest {

    @Mock
    private OptionTemplateGroupRepository groupRepository;

    @Mock
    private OptionTemplateValueRepository valueRepository;

    @InjectMocks
    private OptionTemplateGroupService groupService;

    private OptionTemplateGroup testGroup;
    private OptionTemplateValue testValue;
    private CreateOptionTemplateGroupRequest createRequest;
    private UpdateOptionTemplateGroupRequest updateRequest;

    @BeforeEach
    void setUp() {
        testGroup = OptionTemplateGroup.builder()
                .id(1L)
                .name("甜度")
                .minSelections(1)
                .maxSelections(1)
                .sortOrder(0)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testValue = OptionTemplateValue.builder()
                .id(1L)
                .groupId(1L)
                .name("半糖")
                .priceAdjustment(BigDecimal.ZERO)
                .sortOrder(0)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateOptionTemplateGroupRequest.builder()
                .name("加料")
                .minSelections(0)
                .maxSelections(3)
                .sortOrder(1)
                .build();

        updateRequest = UpdateOptionTemplateGroupRequest.builder()
                .id(1L)
                .name("更新甜度")
                .minSelections(1)
                .maxSelections(1)
                .sortOrder(2)
                .build();
    }

    @Test
    @DisplayName("建立群組 - 成功")
    void createGroup_Success() {
        // Given
        when(groupRepository.existsByName(anyString())).thenReturn(false);
        when(groupRepository.save(any(OptionTemplateGroup.class))).thenAnswer(invocation -> {
            OptionTemplateGroup g = invocation.getArgument(0);
            g.setId(2L);
            g.setCreatedAt(LocalDateTime.now());
            g.setUpdatedAt(LocalDateTime.now());
            return g;
        });

        // When
        OptionTemplateGroupDTO result = groupService.createGroup(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(createRequest.getName());
        assertThat(result.getMinSelections()).isEqualTo(createRequest.getMinSelections());
        assertThat(result.getMaxSelections()).isEqualTo(createRequest.getMaxSelections());
        verify(groupRepository).existsByName(createRequest.getName());
        verify(groupRepository).save(any(OptionTemplateGroup.class));
    }

    @Test
    @DisplayName("建立群組 - 名稱重複拋出例外")
    void createGroup_DuplicateName_ThrowsException() {
        // Given
        when(groupRepository.existsByName(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> groupService.createGroup(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("群組名稱已存在");

        verify(groupRepository).existsByName(createRequest.getName());
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("建立群組 - minSelections > maxSelections 拋出例外")
    void createGroup_InvalidSelections_ThrowsException() {
        // Given
        CreateOptionTemplateGroupRequest invalidRequest = CreateOptionTemplateGroupRequest.builder()
                .name("測試")
                .minSelections(5)
                .maxSelections(1)
                .build();

        // When & Then
        assertThatThrownBy(() -> groupService.createGroup(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("最少選擇數不可大於最多選擇數");

        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新群組 - 成功")
    void updateGroup_Success() {
        // Given
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(groupRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(groupRepository.save(any(OptionTemplateGroup.class))).thenAnswer(invocation -> {
            OptionTemplateGroup g = invocation.getArgument(0);
            g.setUpdatedAt(LocalDateTime.now());
            return g;
        });

        // When
        OptionTemplateGroupDTO result = groupService.updateGroup(updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(groupRepository).findById(1L);
        verify(groupRepository).save(any(OptionTemplateGroup.class));
    }

    @Test
    @DisplayName("更新群組 - 群組不存在拋出例外")
    void updateGroup_NotFound_ThrowsException() {
        // Given
        when(groupRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> groupService.updateGroup(updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("選項範本群組不存在");

        verify(groupRepository).findById(updateRequest.getId());
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新群組 - 名稱與其他群組重複拋出例外")
    void updateGroup_DuplicateName_ThrowsException() {
        // Given
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(groupRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> groupService.updateGroup(updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("群組名稱已存在");
    }

    @Test
    @DisplayName("刪除群組 - 成功（同時刪除選項值）")
    void deleteGroup_Success() {
        // Given
        when(groupRepository.existsById(1L)).thenReturn(true);
        doNothing().when(valueRepository).deleteByGroupId(1L);
        doNothing().when(groupRepository).deleteById(1L);

        // When
        groupService.deleteGroup(1L);

        // Then
        verify(groupRepository).existsById(1L);
        verify(valueRepository).deleteByGroupId(1L);
        verify(groupRepository).deleteById(1L);
    }

    @Test
    @DisplayName("刪除群組 - 群組不存在拋出例外")
    void deleteGroup_NotFound_ThrowsException() {
        // Given
        when(groupRepository.existsById(anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> groupService.deleteGroup(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("選項範本群組不存在");

        verify(groupRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("查詢群組詳情 - 成功（含選項值）")
    void getGroupDetailById_Success() {
        // Given
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(valueRepository.findByGroupIdOrderBySortOrder(1L)).thenReturn(List.of(testValue));

        // When
        OptionTemplateGroupDetailDTO result = groupService.getGroupDetailById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testGroup.getId());
        assertThat(result.getName()).isEqualTo(testGroup.getName());
        assertThat(result.getValues()).hasSize(1);
        assertThat(result.getValues().get(0).getName()).isEqualTo("半糖");
    }

    @Test
    @DisplayName("查詢群組 - 群組不存在拋出例外")
    void getGroupById_NotFound_ThrowsException() {
        // Given
        when(groupRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> groupService.getGroupById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("選項範本群組不存在");
    }

    @Test
    @DisplayName("分頁查詢群組列表")
    void listGroups_WithPagination() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<OptionTemplateGroup> groupPage = new PageImpl<>(List.of(testGroup));
        when(groupRepository.findAll(any(Pageable.class))).thenReturn(groupPage);

        // When
        PageResponse<OptionTemplateGroupDTO> result = groupService.listGroups(pageableRequest, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("分頁查詢群組 - 篩選啟用狀態")
    void listGroups_FilterByIsActive() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<OptionTemplateGroup> groupPage = new PageImpl<>(List.of(testGroup));
        when(groupRepository.findByIsActive(eq(true), any(Pageable.class))).thenReturn(groupPage);

        // When
        PageResponse<OptionTemplateGroupDTO> result = groupService.listGroups(pageableRequest, true);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(groupRepository).findByIsActive(eq(true), any(Pageable.class));
    }

    @Test
    @DisplayName("啟用群組 - 成功")
    void activateGroup_Success() {
        // Given
        testGroup.setIsActive(false);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(groupRepository.save(any(OptionTemplateGroup.class))).thenAnswer(invocation -> {
            OptionTemplateGroup g = invocation.getArgument(0);
            g.setIsActive(true);
            return g;
        });

        // When
        OptionTemplateGroupDTO result = groupService.activateGroup(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("停用群組 - 成功")
    void deactivateGroup_Success() {
        // Given
        testGroup.setIsActive(true);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(groupRepository.save(any(OptionTemplateGroup.class))).thenAnswer(invocation -> {
            OptionTemplateGroup g = invocation.getArgument(0);
            g.setIsActive(false);
            return g;
        });

        // When
        OptionTemplateGroupDTO result = groupService.deactivateGroup(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isFalse();
    }
}
