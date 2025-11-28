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
@DisplayName("ComboService 單元測試")
class ComboServiceTest {

    @Mock
    private ComboRepository comboRepository;

    @Mock
    private ComboItemRepository comboItemRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private ProductOptionGroupService productOptionGroupService;

    @InjectMocks
    private ComboService comboService;

    private Combo testCombo;
    private ProductCategory testCategory;
    private CreateComboRequest createRequest;
    private UpdateComboRequest updateRequest;

    @BeforeEach
    void setUp() {
        testCategory = ProductCategory.builder()
                .id(1L)
                .name("早餐套餐")
                .isActive(true)
                .build();

        testCombo = Combo.builder()
                .id(1L)
                .name("超值早餐A")
                .description("漢堡+飲料組合")
                .price(new BigDecimal("79.00"))
                .imageUrl("http://example.com/combo.jpg")
                .categoryId(null)
                .categoryName(null)
                .isActive(true)
                .sortOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateComboRequest.builder()
                .name("新套餐")
                .description("新套餐說明")
                .price(new BigDecimal("99.00"))
                .imageUrl("http://example.com/new.jpg")
                .categoryId(null)
                .sortOrder(2)
                .build();

        updateRequest = UpdateComboRequest.builder()
                .name("更新套餐")
                .description("更新說明")
                .price(new BigDecimal("89.00"))
                .imageUrl("http://example.com/updated.jpg")
                .categoryId(null)
                .sortOrder(3)
                .build();
    }

    @Test
    @DisplayName("建立套餐 - 成功（無分類）")
    void createCombo_Success() {
        // Given
        when(comboRepository.existsByName(anyString())).thenReturn(false);
        when(comboRepository.save(any(Combo.class))).thenAnswer(invocation -> {
            Combo c = invocation.getArgument(0);
            c.setId(1L);
            c.setCreatedAt(LocalDateTime.now());
            c.setUpdatedAt(LocalDateTime.now());
            return c;
        });

        // When
        ComboDTO result = comboService.createCombo(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(createRequest.getName());
        assertThat(result.getPrice()).isEqualByComparingTo(createRequest.getPrice());
        assertThat(result.getCategoryId()).isNull();
        verify(comboRepository).existsByName(createRequest.getName());
        verify(comboRepository).save(any(Combo.class));
    }

    @Test
    @DisplayName("建立套餐 - 帶分類成功")
    void createCombo_WithCategory_Success() {
        // Given
        createRequest.setCategoryId(1L);
        when(comboRepository.existsByName(anyString())).thenReturn(false);
        when(productCategoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(comboRepository.save(any(Combo.class))).thenAnswer(invocation -> {
            Combo c = invocation.getArgument(0);
            c.setId(1L);
            c.setCreatedAt(LocalDateTime.now());
            c.setUpdatedAt(LocalDateTime.now());
            return c;
        });

        // When
        ComboDTO result = comboService.createCombo(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCategoryId()).isEqualTo(1L);
        assertThat(result.getCategoryName()).isEqualTo("早餐套餐");
        verify(productCategoryRepository).findById(1L);
    }

    @Test
    @DisplayName("建立套餐 - 分類不存在拋出例外")
    void createCombo_CategoryNotFound_ThrowsException() {
        // Given
        createRequest.setCategoryId(999L);
        when(comboRepository.existsByName(anyString())).thenReturn(false);
        when(productCategoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> comboService.createCombo(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("商品分類不存在");

        verify(comboRepository, never()).save(any());
    }

    @Test
    @DisplayName("建立套餐 - 名稱重複拋出例外")
    void createCombo_DuplicateName_ThrowsException() {
        // Given
        when(comboRepository.existsByName(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> comboService.createCombo(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("套餐名稱已存在");

        verify(comboRepository).existsByName(createRequest.getName());
        verify(comboRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新套餐 - 成功")
    void updateCombo_Success() {
        // Given
        when(comboRepository.findById(1L)).thenReturn(Optional.of(testCombo));
        when(comboRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(comboRepository.save(any(Combo.class))).thenReturn(testCombo);

        // When
        ComboDTO result = comboService.updateCombo(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(comboRepository).findById(1L);
        verify(comboRepository).save(any(Combo.class));
    }

    @Test
    @DisplayName("更新套餐 - 套餐不存在拋出例外")
    void updateCombo_NotFound_ThrowsException() {
        // Given
        when(comboRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> comboService.updateCombo(999L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("套餐不存在");

        verify(comboRepository).findById(999L);
        verify(comboRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新套餐 - 名稱與其他套餐重複拋出例外")
    void updateCombo_DuplicateName_ThrowsException() {
        // Given
        when(comboRepository.findById(1L)).thenReturn(Optional.of(testCombo));
        when(comboRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> comboService.updateCombo(1L, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("套餐名稱已存在");
    }

    @Test
    @DisplayName("刪除套餐 - 成功（同時刪除項目）")
    void deleteCombo_Success() {
        // Given
        when(comboRepository.existsById(1L)).thenReturn(true);
        doNothing().when(comboItemRepository).deleteByComboId(1L);
        doNothing().when(comboRepository).deleteById(1L);

        // When
        comboService.deleteCombo(1L);

        // Then
        verify(comboRepository).existsById(1L);
        verify(comboItemRepository).deleteByComboId(1L);
        verify(comboRepository).deleteById(1L);
    }

    @Test
    @DisplayName("刪除套餐 - 套餐不存在拋出例外")
    void deleteCombo_NotFound_ThrowsException() {
        // Given
        when(comboRepository.existsById(anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> comboService.deleteCombo(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("套餐不存在");

        verify(comboRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("查詢套餐 - 成功")
    void getComboById_Success() {
        // Given
        when(comboRepository.findById(1L)).thenReturn(Optional.of(testCombo));

        // When
        ComboDTO result = comboService.getComboById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testCombo.getId());
        assertThat(result.getName()).isEqualTo(testCombo.getName());
    }

    @Test
    @DisplayName("查詢套餐 - 套餐不存在拋出例外")
    void getComboById_NotFound_ThrowsException() {
        // Given
        when(comboRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> comboService.getComboById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("套餐不存在");
    }

    @Test
    @DisplayName("查詢套餐詳情 - 含項目與選項")
    void getComboDetailById_WithItems_Success() {
        // Given
        ComboItem item = ComboItem.builder()
                .id(1L)
                .comboId(1L)
                .productId(100L)
                .productName("招牌漢堡")
                .quantity(1)
                .sortOrder(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(comboRepository.findById(1L)).thenReturn(Optional.of(testCombo));
        when(comboItemRepository.findByComboIdOrderBySortOrder(1L)).thenReturn(List.of(item));
        when(productOptionGroupService.listGroupsWithValuesByProductId(100L))
                .thenReturn(List.of());

        // When
        ComboDetailDTO result = comboService.getComboDetailById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("招牌漢堡");
        verify(productOptionGroupService).listGroupsWithValuesByProductId(100L);
    }

    @Test
    @DisplayName("分頁查詢套餐列表")
    void listCombos_WithPagination() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Combo> comboPage = new PageImpl<>(List.of(testCombo));
        when(comboRepository.findAll(any(Pageable.class))).thenReturn(comboPage);

        // When
        PageResponse<ComboDTO> result = comboService.listCombos(pageableRequest, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("分頁查詢套餐 - 篩選啟用狀態")
    void listCombos_FilterByIsActive() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Combo> comboPage = new PageImpl<>(List.of(testCombo));
        when(comboRepository.findByIsActive(eq(true), any(Pageable.class))).thenReturn(comboPage);

        // When
        PageResponse<ComboDTO> result = comboService.listCombos(pageableRequest, true, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(comboRepository).findByIsActive(eq(true), any(Pageable.class));
    }

    @Test
    @DisplayName("分頁查詢套餐 - 按分類篩選")
    void listCombos_FilterByCategory() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        testCombo.setCategoryId(1L);
        testCombo.setCategoryName("早餐套餐");
        Page<Combo> comboPage = new PageImpl<>(List.of(testCombo));
        when(comboRepository.findByCategoryId(eq(1L), any(Pageable.class))).thenReturn(comboPage);

        // When
        PageResponse<ComboDTO> result = comboService.listCombos(pageableRequest, null, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategoryName()).isEqualTo("早餐套餐");
        verify(comboRepository).findByCategoryId(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("啟用套餐 - 成功")
    void activateCombo_Success() {
        // Given
        testCombo.setIsActive(false);
        when(comboRepository.findById(1L)).thenReturn(Optional.of(testCombo));
        when(comboRepository.save(any(Combo.class))).thenAnswer(invocation -> {
            Combo c = invocation.getArgument(0);
            c.setIsActive(true);
            return c;
        });

        // When
        ComboDTO result = comboService.activateCombo(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("停用套餐 - 成功")
    void deactivateCombo_Success() {
        // Given
        testCombo.setIsActive(true);
        when(comboRepository.findById(1L)).thenReturn(Optional.of(testCombo));
        when(comboRepository.save(any(Combo.class))).thenAnswer(invocation -> {
            Combo c = invocation.getArgument(0);
            c.setIsActive(false);
            return c;
        });

        // When
        ComboDTO result = comboService.deactivateCombo(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isFalse();
    }
}
