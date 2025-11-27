package com.morningharvest.erp.product.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.event.EventPublisher;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.product.dto.CreateProductCategoryRequest;
import com.morningharvest.erp.product.dto.ProductCategoryDTO;
import com.morningharvest.erp.product.dto.UpdateProductCategoryRequest;
import com.morningharvest.erp.product.entity.ProductCategory;
import com.morningharvest.erp.product.event.ProductCategoryUpdatedEvent;
import com.morningharvest.erp.product.repository.ProductCategoryRepository;
import com.morningharvest.erp.product.repository.ProductRepository;
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
@DisplayName("ProductCategoryService 單元測試")
class ProductCategoryServiceTest {

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ProductCategoryService productCategoryService;

    private ProductCategory testCategory;
    private CreateProductCategoryRequest createRequest;
    private UpdateProductCategoryRequest updateRequest;

    @BeforeEach
    void setUp() {
        testCategory = ProductCategory.builder()
                .id(1L)
                .name("測試分類")
                .description("測試說明")
                .isActive(true)
                .sortOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateProductCategoryRequest.builder()
                .name("新分類")
                .description("新分類說明")
                .sortOrder(2)
                .build();

        updateRequest = UpdateProductCategoryRequest.builder()
                .id(1L)
                .name("更新分類")
                .description("更新說明")
                .sortOrder(3)
                .build();
    }

    @Test
    @DisplayName("建立分類 - 成功")
    void createCategory_Success() {
        // Given
        when(productCategoryRepository.existsByName(anyString())).thenReturn(false);
        when(productCategoryRepository.save(any(ProductCategory.class))).thenAnswer(invocation -> {
            ProductCategory c = invocation.getArgument(0);
            c.setId(1L);
            c.setCreatedAt(LocalDateTime.now());
            c.setUpdatedAt(LocalDateTime.now());
            return c;
        });

        // When
        ProductCategoryDTO result = productCategoryService.createCategory(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(createRequest.getName());
        verify(productCategoryRepository).existsByName(createRequest.getName());
        verify(productCategoryRepository).save(any(ProductCategory.class));
    }

    @Test
    @DisplayName("建立分類 - 名稱重複拋出例外")
    void createCategory_DuplicateName_ThrowsException() {
        // Given
        when(productCategoryRepository.existsByName(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> productCategoryService.createCategory(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分類名稱已存在");

        verify(productCategoryRepository).existsByName(createRequest.getName());
        verify(productCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新分類 - 成功並發布事件")
    void updateCategory_Success() {
        // Given
        when(productCategoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productCategoryRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(productCategoryRepository.save(any(ProductCategory.class))).thenAnswer(invocation -> {
            ProductCategory c = invocation.getArgument(0);
            c.setUpdatedAt(LocalDateTime.now());
            return c;
        });

        // When
        ProductCategoryDTO result = productCategoryService.updateCategory(updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(productCategoryRepository).findById(1L);
        verify(productCategoryRepository).save(any(ProductCategory.class));
        // 驗證事件被發布
        verify(eventPublisher).publish(any(ProductCategoryUpdatedEvent.class), eq("分類更新"));
    }

    @Test
    @DisplayName("更新分類 - 事件包含正確的 before/after 資料")
    void updateCategory_EventContainsCorrectData() {
        // Given
        String oldName = testCategory.getName();  // "測試分類"
        String newName = updateRequest.getName(); // "更新分類"

        when(productCategoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productCategoryRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(productCategoryRepository.save(any(ProductCategory.class))).thenAnswer(invocation -> {
            ProductCategory c = invocation.getArgument(0);
            c.setUpdatedAt(LocalDateTime.now());
            return c;
        });

        // When
        productCategoryService.updateCategory(updateRequest);

        // Then - 使用 ArgumentCaptor 驗證事件內容
        org.mockito.ArgumentCaptor<ProductCategoryUpdatedEvent> eventCaptor =
                org.mockito.ArgumentCaptor.forClass(ProductCategoryUpdatedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture(), eq("分類更新"));

        ProductCategoryUpdatedEvent event = eventCaptor.getValue();
        assertThat(event.getBefore().getName()).isEqualTo(oldName);
        assertThat(event.getAfter().getName()).isEqualTo(newName);
        assertThat(event.isNameChanged()).isTrue();
        assertThat(event.getOldName()).isEqualTo(oldName);
        assertThat(event.getNewName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("更新分類 - 分類不存在拋出例外")
    void updateCategory_NotFound_ThrowsException() {
        // Given
        when(productCategoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productCategoryService.updateCategory(updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("商品分類不存在");

        verify(productCategoryRepository).findById(updateRequest.getId());
        verify(productCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新分類 - 名稱與其他分類重複拋出例外")
    void updateCategory_DuplicateName_ThrowsException() {
        // Given
        when(productCategoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productCategoryRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> productCategoryService.updateCategory(updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分類名稱已存在");
    }

    @Test
    @DisplayName("刪除分類 - 成功")
    void deleteCategory_Success() {
        // Given
        when(productCategoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);
        doNothing().when(productCategoryRepository).deleteById(1L);

        // When
        productCategoryService.deleteCategory(1L);

        // Then
        verify(productCategoryRepository).existsById(1L);
        verify(productRepository).existsByCategoryId(1L);
        verify(productCategoryRepository).deleteById(1L);
    }

    @Test
    @DisplayName("刪除分類 - 分類下有商品拋出例外")
    void deleteCategory_HasProducts_ThrowsException() {
        // Given
        when(productCategoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> productCategoryService.deleteCategory(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("無法刪除分類，該分類下仍有商品");

        verify(productCategoryRepository).existsById(1L);
        verify(productRepository).existsByCategoryId(1L);
        verify(productCategoryRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("刪除分類 - 分類不存在拋出例外")
    void deleteCategory_NotFound_ThrowsException() {
        // Given
        when(productCategoryRepository.existsById(anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> productCategoryService.deleteCategory(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("商品分類不存在");

        verify(productCategoryRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("查詢分類 - 成功")
    void getCategoryById_Success() {
        // Given
        when(productCategoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // When
        ProductCategoryDTO result = productCategoryService.getCategoryById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testCategory.getId());
        assertThat(result.getName()).isEqualTo(testCategory.getName());
    }

    @Test
    @DisplayName("查詢分類 - 分類不存在拋出例外")
    void getCategoryById_NotFound_ThrowsException() {
        // Given
        when(productCategoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productCategoryService.getCategoryById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("商品分類不存在");
    }

    @Test
    @DisplayName("分頁查詢分類列表")
    void listCategories_WithPagination() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<ProductCategory> categoryPage = new PageImpl<>(List.of(testCategory));
        when(productCategoryRepository.findAll(any(Pageable.class))).thenReturn(categoryPage);

        // When
        PageResponse<ProductCategoryDTO> result = productCategoryService.listCategories(pageableRequest, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("分頁查詢分類 - 篩選啟用狀態")
    void listCategories_FilterByIsActive() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<ProductCategory> categoryPage = new PageImpl<>(List.of(testCategory));
        when(productCategoryRepository.findByIsActive(eq(true), any(Pageable.class))).thenReturn(categoryPage);

        // When
        PageResponse<ProductCategoryDTO> result = productCategoryService.listCategories(pageableRequest, true);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(productCategoryRepository).findByIsActive(eq(true), any(Pageable.class));
    }

    @Test
    @DisplayName("啟用分類 - 成功")
    void activateCategory_Success() {
        // Given
        testCategory.setIsActive(false);
        when(productCategoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productCategoryRepository.save(any(ProductCategory.class))).thenAnswer(invocation -> {
            ProductCategory c = invocation.getArgument(0);
            c.setIsActive(true);
            return c;
        });

        // When
        ProductCategoryDTO result = productCategoryService.activateCategory(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("停用分類 - 成功")
    void deactivateCategory_Success() {
        // Given
        testCategory.setIsActive(true);
        when(productCategoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productCategoryRepository.save(any(ProductCategory.class))).thenAnswer(invocation -> {
            ProductCategory c = invocation.getArgument(0);
            c.setIsActive(false);
            return c;
        });

        // When
        ProductCategoryDTO result = productCategoryService.deactivateCategory(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isFalse();
    }
}
