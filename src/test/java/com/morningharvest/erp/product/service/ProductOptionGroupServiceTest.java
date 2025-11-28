package com.morningharvest.erp.product.service;

import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.product.dto.*;
import com.morningharvest.erp.product.entity.ProductOptionGroup;
import com.morningharvest.erp.product.entity.ProductOptionValue;
import com.morningharvest.erp.product.repository.ProductOptionGroupRepository;
import com.morningharvest.erp.product.repository.ProductOptionValueRepository;
import com.morningharvest.erp.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductOptionGroupService 單元測試")
class ProductOptionGroupServiceTest {

    @Mock
    private ProductOptionGroupRepository groupRepository;

    @Mock
    private ProductOptionValueRepository valueRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductOptionGroupService groupService;

    private ProductOptionGroup testGroup;
    private ProductOptionValue testValue;
    private CreateProductOptionGroupRequest createRequest;
    private UpdateProductOptionGroupRequest updateRequest;

    @BeforeEach
    void setUp() {
        testGroup = ProductOptionGroup.builder()
                .id(1L)
                .productId(100L)
                .name("甜度")
                .minSelections(1)
                .maxSelections(1)
                .sortOrder(0)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testValue = ProductOptionValue.builder()
                .id(1L)
                .groupId(1L)
                .name("半糖")
                .priceAdjustment(BigDecimal.ZERO)
                .sortOrder(0)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateProductOptionGroupRequest.builder()
                .productId(100L)
                .name("加料")
                .minSelections(0)
                .maxSelections(3)
                .sortOrder(1)
                .build();

        updateRequest = UpdateProductOptionGroupRequest.builder()
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
        when(productRepository.existsById(100L)).thenReturn(true);
        when(groupRepository.existsByProductIdAndName(anyLong(), anyString())).thenReturn(false);
        when(groupRepository.save(any(ProductOptionGroup.class))).thenAnswer(invocation -> {
            ProductOptionGroup g = invocation.getArgument(0);
            g.setId(2L);
            g.setCreatedAt(LocalDateTime.now());
            g.setUpdatedAt(LocalDateTime.now());
            return g;
        });

        // When
        ProductOptionGroupDTO result = groupService.createGroup(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(createRequest.getProductId());
        assertThat(result.getName()).isEqualTo(createRequest.getName());
        assertThat(result.getMinSelections()).isEqualTo(createRequest.getMinSelections());
        assertThat(result.getMaxSelections()).isEqualTo(createRequest.getMaxSelections());
        verify(productRepository).existsById(100L);
        verify(groupRepository).existsByProductIdAndName(100L, createRequest.getName());
        verify(groupRepository).save(any(ProductOptionGroup.class));
    }

    @Test
    @DisplayName("建立群組 - 產品不存在拋出例外")
    void createGroup_ProductNotFound_ThrowsException() {
        // Given
        when(productRepository.existsById(anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> groupService.createGroup(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("產品不存在");

        verify(productRepository).existsById(createRequest.getProductId());
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("建立群組 - 名稱重複拋出例外")
    void createGroup_DuplicateName_ThrowsException() {
        // Given
        when(productRepository.existsById(100L)).thenReturn(true);
        when(groupRepository.existsByProductIdAndName(anyLong(), anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> groupService.createGroup(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("此產品已有相同名稱的選項群組");

        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("建立群組 - minSelections > maxSelections 拋出例外")
    void createGroup_InvalidSelections_ThrowsException() {
        // Given
        CreateProductOptionGroupRequest invalidRequest = CreateProductOptionGroupRequest.builder()
                .productId(100L)
                .name("測試")
                .minSelections(5)
                .maxSelections(1)
                .build();
        when(productRepository.existsById(100L)).thenReturn(true);

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
        when(groupRepository.existsByProductIdAndNameAndIdNot(anyLong(), anyString(), anyLong())).thenReturn(false);
        when(groupRepository.save(any(ProductOptionGroup.class))).thenAnswer(invocation -> {
            ProductOptionGroup g = invocation.getArgument(0);
            g.setUpdatedAt(LocalDateTime.now());
            return g;
        });

        // When
        ProductOptionGroupDTO result = groupService.updateGroup(updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(groupRepository).findById(1L);
        verify(groupRepository).save(any(ProductOptionGroup.class));
    }

    @Test
    @DisplayName("更新群組 - 群組不存在拋出例外")
    void updateGroup_NotFound_ThrowsException() {
        // Given
        when(groupRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> groupService.updateGroup(updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("產品選項群組不存在");

        verify(groupRepository).findById(updateRequest.getId());
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新群組 - 名稱與其他群組重複拋出例外")
    void updateGroup_DuplicateName_ThrowsException() {
        // Given
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(groupRepository.existsByProductIdAndNameAndIdNot(anyLong(), anyString(), anyLong())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> groupService.updateGroup(updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("此產品已有相同名稱的選項群組");
    }

    @Test
    @DisplayName("更新群組 - minSelections > maxSelections 拋出例外")
    void updateGroup_InvalidSelections_ThrowsException() {
        // Given
        UpdateProductOptionGroupRequest invalidRequest = UpdateProductOptionGroupRequest.builder()
                .id(1L)
                .name("測試")
                .minSelections(5)
                .maxSelections(1)
                .build();
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        // When & Then
        assertThatThrownBy(() -> groupService.updateGroup(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("最少選擇數不可大於最多選擇數");
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
                .hasMessageContaining("產品選項群組不存在");

        verify(groupRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("查詢群組詳情 - 成功（含選項值）")
    void getGroupDetailById_Success() {
        // Given
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(valueRepository.findByGroupIdOrderBySortOrder(1L)).thenReturn(List.of(testValue));

        // When
        ProductOptionGroupDetailDTO result = groupService.getGroupDetailById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testGroup.getId());
        assertThat(result.getProductId()).isEqualTo(testGroup.getProductId());
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
                .hasMessageContaining("產品選項群組不存在");
    }

    @Test
    @DisplayName("查詢產品選項群組列表 - 成功")
    void listGroupsByProductId_Success() {
        // Given
        when(productRepository.existsById(100L)).thenReturn(true);
        when(groupRepository.findByProductIdOrderBySortOrder(100L)).thenReturn(List.of(testGroup));

        // When
        List<ProductOptionGroupDTO> result = groupService.listGroupsByProductId(100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("甜度");
    }

    @Test
    @DisplayName("查詢產品選項群組列表 - 產品不存在拋出例外")
    void listGroupsByProductId_ProductNotFound_ThrowsException() {
        // Given
        when(productRepository.existsById(anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> groupService.listGroupsByProductId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("產品不存在");
    }

    @Test
    @DisplayName("查詢產品選項群組列表（含選項值）- 成功")
    void listGroupsWithValuesByProductId_Success() {
        // Given
        when(productRepository.existsById(100L)).thenReturn(true);
        when(groupRepository.findByProductIdOrderBySortOrder(100L)).thenReturn(List.of(testGroup));
        when(valueRepository.findByGroupIdInOrderByGroupIdAndSortOrder(List.of(1L))).thenReturn(List.of(testValue));

        // When
        List<ProductOptionGroupDetailDTO> result = groupService.listGroupsWithValuesByProductId(100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("甜度");
        assertThat(result.get(0).getValues()).hasSize(1);
        assertThat(result.get(0).getValues().get(0).getName()).isEqualTo("半糖");
    }

    @Test
    @DisplayName("查詢產品選項群組列表（含選項值）- 空列表")
    void listGroupsWithValuesByProductId_EmptyList() {
        // Given
        when(productRepository.existsById(100L)).thenReturn(true);
        when(groupRepository.findByProductIdOrderBySortOrder(100L)).thenReturn(List.of());

        // When
        List<ProductOptionGroupDetailDTO> result = groupService.listGroupsWithValuesByProductId(100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(valueRepository, never()).findByGroupIdInOrderByGroupIdAndSortOrder(any());
    }

    @Test
    @DisplayName("啟用群組 - 成功")
    void activateGroup_Success() {
        // Given
        testGroup.setIsActive(false);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(groupRepository.save(any(ProductOptionGroup.class))).thenAnswer(invocation -> {
            ProductOptionGroup g = invocation.getArgument(0);
            g.setIsActive(true);
            return g;
        });

        // When
        ProductOptionGroupDTO result = groupService.activateGroup(1L);

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
        when(groupRepository.save(any(ProductOptionGroup.class))).thenAnswer(invocation -> {
            ProductOptionGroup g = invocation.getArgument(0);
            g.setIsActive(false);
            return g;
        });

        // When
        ProductOptionGroupDTO result = groupService.deactivateGroup(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isFalse();
    }
}
