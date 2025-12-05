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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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
@DisplayName("MaterialService 單元測試")
class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MaterialService materialService;

    private Material testMaterial;
    private CreateMaterialRequest createRequest;
    private UpdateMaterialRequest updateRequest;

    @BeforeEach
    void setUp() {
        testMaterial = Material.builder()
                .id(1L)
                .code("M001")
                .name("測試原物料")
                .unit(MaterialUnit.PIECE)
                .category(MaterialCategory.OTHER)
                .specification("測試規格")
                .safeStockQuantity(BigDecimal.TEN)
                .currentStockQuantity(new BigDecimal("50.00"))
                .costPrice(new BigDecimal("25.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateMaterialRequest.builder()
                .code("M002")
                .name("新原物料")
                .unit(MaterialUnit.KILOGRAM)
                .category(MaterialCategory.MEAT)
                .specification("新規格")
                .safeStockQuantity(new BigDecimal("20.00"))
                .currentStockQuantity(new BigDecimal("100.00"))
                .costPrice(new BigDecimal("50.00"))
                .note("測試備註")
                .build();

        updateRequest = UpdateMaterialRequest.builder()
                .id(1L)
                .code("M001")
                .name("更新原物料")
                .unit(MaterialUnit.PACK)
                .category(MaterialCategory.BREAD)
                .specification("更新規格")
                .safeStockQuantity(new BigDecimal("30.00"))
                .currentStockQuantity(new BigDecimal("150.00"))
                .costPrice(new BigDecimal("75.00"))
                .note("更新備註")
                .build();
    }

    // ===== createMaterial 測試 =====

    @Test
    @DisplayName("建立原物料 - 成功")
    void createMaterial_Success() {
        // Given
        when(materialRepository.existsByCode(anyString())).thenReturn(false);
        when(materialRepository.existsByName(anyString())).thenReturn(false);
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> {
            Material m = invocation.getArgument(0);
            m.setId(1L);
            m.setCreatedAt(LocalDateTime.now());
            m.setUpdatedAt(LocalDateTime.now());
            return m;
        });

        // When
        MaterialDTO result = materialService.createMaterial(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(createRequest.getCode());
        assertThat(result.getName()).isEqualTo(createRequest.getName());
        assertThat(result.getUnit()).isEqualTo(createRequest.getUnit());
        assertThat(result.getUnitDisplayName()).isEqualTo("公斤");
        assertThat(result.getCategory()).isEqualTo(createRequest.getCategory());
        assertThat(result.getCategoryDisplayName()).isEqualTo("肉類");
        assertThat(result.getCostPrice()).isEqualByComparingTo(createRequest.getCostPrice());
        verify(materialRepository).existsByCode(createRequest.getCode());
        verify(materialRepository).existsByName(createRequest.getName());
        verify(materialRepository).save(any(Material.class));
    }

    @Test
    @DisplayName("建立原物料 - 編號重複拋出例外")
    void createMaterial_DuplicateCode_ThrowsException() {
        // Given
        when(materialRepository.existsByCode(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> materialService.createMaterial(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("原物料編號已存在");

        verify(materialRepository).existsByCode(createRequest.getCode());
        verify(materialRepository, never()).save(any());
    }

    @Test
    @DisplayName("建立原物料 - 名稱重複拋出例外")
    void createMaterial_DuplicateName_ThrowsException() {
        // Given
        when(materialRepository.existsByCode(anyString())).thenReturn(false);
        when(materialRepository.existsByName(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> materialService.createMaterial(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("原物料名稱已存在");

        verify(materialRepository, never()).save(any());
    }

    @Test
    @DisplayName("建立原物料 - 無效單位拋出例外")
    void createMaterial_InvalidUnit_ThrowsException() {
        // Given
        createRequest.setUnit("INVALID_UNIT");
        when(materialRepository.existsByCode(anyString())).thenReturn(false);
        when(materialRepository.existsByName(anyString())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> materialService.createMaterial(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("無效的單位");

        verify(materialRepository, never()).save(any());
    }

    @Test
    @DisplayName("建立原物料 - 無效分類拋出例外")
    void createMaterial_InvalidCategory_ThrowsException() {
        // Given
        createRequest.setCategory("INVALID_CATEGORY");
        when(materialRepository.existsByCode(anyString())).thenReturn(false);
        when(materialRepository.existsByName(anyString())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> materialService.createMaterial(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("無效的分類");

        verify(materialRepository, never()).save(any());
    }

    // ===== updateMaterial 測試 =====

    @Test
    @DisplayName("更新原物料 - 成功")
    void updateMaterial_Success() {
        // Given
        when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
        when(materialRepository.existsByCodeAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(materialRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(materialRepository.save(any(Material.class))).thenReturn(testMaterial);

        // When
        MaterialDTO result = materialService.updateMaterial(updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(materialRepository).findById(1L);
        verify(materialRepository).save(any(Material.class));
    }

    @Test
    @DisplayName("更新原物料 - 原物料不存在拋出例外")
    void updateMaterial_NotFound_ThrowsException() {
        // Given
        when(materialRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> materialService.updateMaterial(updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("原物料不存在");

        verify(materialRepository).findById(updateRequest.getId());
        verify(materialRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新原物料 - 編號與其他重複拋出例外")
    void updateMaterial_DuplicateCode_ThrowsException() {
        // Given
        when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
        when(materialRepository.existsByCodeAndIdNot(anyString(), anyLong())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> materialService.updateMaterial(updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("原物料編號已存在");

        verify(materialRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新原物料 - 名稱與其他重複拋出例外")
    void updateMaterial_DuplicateName_ThrowsException() {
        // Given
        when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
        when(materialRepository.existsByCodeAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(materialRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> materialService.updateMaterial(updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("原物料名稱已存在");

        verify(materialRepository, never()).save(any());
    }

    // ===== deleteMaterial 測試 =====

    @Test
    @DisplayName("停用原物料 - 成功")
    void deleteMaterial_Success() {
        // Given
        when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> {
            Material m = invocation.getArgument(0);
            assertThat(m.getIsActive()).isFalse();
            return m;
        });

        // When
        materialService.deleteMaterial(1L);

        // Then
        verify(materialRepository).findById(1L);
        verify(materialRepository).save(any(Material.class));
    }

    @Test
    @DisplayName("停用原物料 - 原物料不存在拋出例外")
    void deleteMaterial_NotFound_ThrowsException() {
        // Given
        when(materialRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> materialService.deleteMaterial(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("原物料不存在");

        verify(materialRepository, never()).save(any());
    }

    // ===== getMaterialById 測試 =====

    @Test
    @DisplayName("查詢原物料 - 成功")
    void getMaterialById_Success() {
        // Given
        when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));

        // When
        MaterialDTO result = materialService.getMaterialById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testMaterial.getId());
        assertThat(result.getCode()).isEqualTo(testMaterial.getCode());
        assertThat(result.getName()).isEqualTo(testMaterial.getName());
        assertThat(result.getUnitDisplayName()).isEqualTo("個");
    }

    @Test
    @DisplayName("查詢原物料 - 原物料不存在拋出例外")
    void getMaterialById_NotFound_ThrowsException() {
        // Given
        when(materialRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> materialService.getMaterialById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("原物料不存在");
    }

    // ===== listMaterials 測試 =====

    @Test
    @DisplayName("分頁查詢原物料列表")
    void listMaterials_WithPagination() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Material> materialPage = new PageImpl<>(List.of(testMaterial));
        when(materialRepository.findAll(any(Pageable.class))).thenReturn(materialPage);

        // When
        PageResponse<MaterialDTO> result = materialService.listMaterials(pageableRequest, null, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("分頁查詢原物料 - 篩選啟用狀態")
    void listMaterials_FilterByIsActive() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Material> materialPage = new PageImpl<>(List.of(testMaterial));
        when(materialRepository.findByIsActive(eq(true), any(Pageable.class))).thenReturn(materialPage);

        // When
        PageResponse<MaterialDTO> result = materialService.listMaterials(pageableRequest, true, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(materialRepository).findByIsActive(eq(true), any(Pageable.class));
    }

    @Test
    @DisplayName("分頁查詢原物料 - 按分類篩選")
    void listMaterials_FilterByCategory() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Material> materialPage = new PageImpl<>(List.of(testMaterial));
        when(materialRepository.findByCategory(eq(MaterialCategory.OTHER), any(Pageable.class)))
                .thenReturn(materialPage);

        // When
        PageResponse<MaterialDTO> result = materialService.listMaterials(
                pageableRequest, null, MaterialCategory.OTHER, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(materialRepository).findByCategory(eq(MaterialCategory.OTHER), any(Pageable.class));
    }

    @Test
    @DisplayName("分頁查詢原物料 - 關鍵字搜尋")
    void listMaterials_FilterByKeyword() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Material> materialPage = new PageImpl<>(List.of(testMaterial));
        when(materialRepository.findByNameContaining(eq("測試"), any(Pageable.class)))
                .thenReturn(materialPage);

        // When
        PageResponse<MaterialDTO> result = materialService.listMaterials(pageableRequest, null, null, "測試");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(materialRepository).findByNameContaining(eq("測試"), any(Pageable.class));
    }

    @Test
    @DisplayName("分頁查詢原物料 - 組合篩選")
    void listMaterials_CombinedFilter() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Material> materialPage = new PageImpl<>(List.of(testMaterial));
        when(materialRepository.findByCategoryAndIsActive(
                eq(MaterialCategory.OTHER), eq(true), any(Pageable.class)))
                .thenReturn(materialPage);

        // When
        PageResponse<MaterialDTO> result = materialService.listMaterials(
                pageableRequest, true, MaterialCategory.OTHER, null);

        // Then
        assertThat(result).isNotNull();
        verify(materialRepository).findByCategoryAndIsActive(
                eq(MaterialCategory.OTHER), eq(true), any(Pageable.class));
    }

    // ===== activateMaterial 測試 =====

    @Test
    @DisplayName("啟用原物料 - 成功")
    void activateMaterial_Success() {
        // Given
        testMaterial.setIsActive(false);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> {
            Material m = invocation.getArgument(0);
            m.setIsActive(true);
            return m;
        });

        // When
        MaterialDTO result = materialService.activateMaterial(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("啟用原物料 - 原物料不存在拋出例外")
    void activateMaterial_NotFound_ThrowsException() {
        // Given
        when(materialRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> materialService.activateMaterial(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("原物料不存在");
    }
}
