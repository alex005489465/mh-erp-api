package com.morningharvest.erp.product.service;

import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.common.test.TestDataFactory;
import com.morningharvest.erp.material.entity.Material;
import com.morningharvest.erp.material.repository.MaterialRepository;
import com.morningharvest.erp.product.dto.CreateProductRecipeRequest;
import com.morningharvest.erp.product.dto.ProductRecipeDTO;
import com.morningharvest.erp.product.dto.UpdateProductRecipeRequest;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.entity.ProductRecipe;
import com.morningharvest.erp.product.repository.ProductRecipeRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductRecipeService 單元測試")
class ProductRecipeServiceTest {

    @Mock
    private ProductRecipeRepository productRecipeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private ProductRecipeService productRecipeService;

    private Product testProduct;
    private Material testMaterial;
    private ProductRecipe testRecipe;
    private CreateProductRecipeRequest createRequest;
    private UpdateProductRecipeRequest updateRequest;

    @BeforeEach
    void setUp() {
        testProduct = TestDataFactory.defaultProduct()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testMaterial = TestDataFactory.defaultMaterial()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testRecipe = TestDataFactory.defaultProductRecipe()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateProductRecipeRequest.builder()
                .productId(1L)
                .materialId(1L)
                .quantity(new BigDecimal("2.0000"))
                .note("測試備註")
                .build();

        updateRequest = UpdateProductRecipeRequest.builder()
                .id(1L)
                .quantity(new BigDecimal("3.0000"))
                .note("更新備註")
                .build();
    }

    // ===== createRecipe 測試 =====

    @Test
    @DisplayName("新增配方 - 成功")
    void createRecipe_Success() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
        when(productRecipeRepository.existsByProductIdAndMaterialId(1L, 1L)).thenReturn(false);
        when(productRecipeRepository.save(any(ProductRecipe.class))).thenAnswer(invocation -> {
            ProductRecipe recipe = invocation.getArgument(0);
            recipe.setId(1L);
            recipe.setCreatedAt(LocalDateTime.now());
            recipe.setUpdatedAt(LocalDateTime.now());
            return recipe;
        });

        // When
        ProductRecipeDTO result = productRecipeService.createRecipe(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getProductName()).isEqualTo(testProduct.getName());
        assertThat(result.getMaterialId()).isEqualTo(1L);
        assertThat(result.getMaterialCode()).isEqualTo(testMaterial.getCode());
        assertThat(result.getMaterialName()).isEqualTo(testMaterial.getName());
        assertThat(result.getQuantity()).isEqualByComparingTo(createRequest.getQuantity());
        assertThat(result.getUnit()).isEqualTo(testMaterial.getUnit());

        verify(productRepository).findById(1L);
        verify(materialRepository).findById(1L);
        verify(productRecipeRepository).existsByProductIdAndMaterialId(1L, 1L);
        verify(productRecipeRepository).save(any(ProductRecipe.class));
    }

    @Test
    @DisplayName("新增配方 - 商品不存在拋出例外")
    void createRecipe_ProductNotFound_ThrowsException() {
        // Given
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productRecipeService.createRecipe(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("商品不存在");

        verify(productRepository).findById(1L);
        verify(materialRepository, never()).findById(anyLong());
        verify(productRecipeRepository, never()).save(any());
    }

    @Test
    @DisplayName("新增配方 - 原物料不存在拋出例外")
    void createRecipe_MaterialNotFound_ThrowsException() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(materialRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productRecipeService.createRecipe(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("原物料不存在");

        verify(productRepository).findById(1L);
        verify(materialRepository).findById(1L);
        verify(productRecipeRepository, never()).save(any());
    }

    @Test
    @DisplayName("新增配方 - 重複配方拋出例外")
    void createRecipe_DuplicateRecipe_ThrowsException() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
        when(productRecipeRepository.existsByProductIdAndMaterialId(1L, 1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> productRecipeService.createRecipe(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("此商品已有該原物料的配方");

        verify(productRecipeRepository, never()).save(any());
    }

    // ===== updateRecipe 測試 =====

    @Test
    @DisplayName("更新配方 - 成功")
    void updateRecipe_Success() {
        // Given
        when(productRecipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        when(productRecipeRepository.save(any(ProductRecipe.class))).thenReturn(testRecipe);

        // When
        ProductRecipeDTO result = productRecipeService.updateRecipe(updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(productRecipeRepository).findById(1L);
        verify(productRecipeRepository).save(any(ProductRecipe.class));
    }

    @Test
    @DisplayName("更新配方 - 配方不存在拋出例外")
    void updateRecipe_NotFound_ThrowsException() {
        // Given
        when(productRecipeRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productRecipeService.updateRecipe(updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("配方不存在");

        verify(productRecipeRepository).findById(1L);
        verify(productRecipeRepository, never()).save(any());
    }

    // ===== deleteRecipe 測試 =====

    @Test
    @DisplayName("刪除配方 - 成功")
    void deleteRecipe_Success() {
        // Given
        when(productRecipeRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRecipeRepository).deleteById(1L);

        // When
        productRecipeService.deleteRecipe(1L);

        // Then
        verify(productRecipeRepository).existsById(1L);
        verify(productRecipeRepository).deleteById(1L);
    }

    @Test
    @DisplayName("刪除配方 - 配方不存在拋出例外")
    void deleteRecipe_NotFound_ThrowsException() {
        // Given
        when(productRecipeRepository.existsById(anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> productRecipeService.deleteRecipe(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("配方不存在");

        verify(productRecipeRepository, never()).deleteById(anyLong());
    }

    // ===== getRecipeById 測試 =====

    @Test
    @DisplayName("查詢配方 - 成功")
    void getRecipeById_Success() {
        // Given
        when(productRecipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));

        // When
        ProductRecipeDTO result = productRecipeService.getRecipeById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testRecipe.getId());
        assertThat(result.getProductId()).isEqualTo(testRecipe.getProductId());
        assertThat(result.getMaterialId()).isEqualTo(testRecipe.getMaterialId());
    }

    @Test
    @DisplayName("查詢配方 - 配方不存在拋出例外")
    void getRecipeById_NotFound_ThrowsException() {
        // Given
        when(productRecipeRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productRecipeService.getRecipeById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("配方不存在");
    }

    // ===== listRecipesByProductId 測試 =====

    @Test
    @DisplayName("查詢商品配方清單 - 成功")
    void listRecipesByProductId_Success() {
        // Given
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRecipeRepository.findByProductId(1L)).thenReturn(List.of(testRecipe));

        // When
        List<ProductRecipeDTO> result = productRecipeService.listRecipesByProductId(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(1L);

        verify(productRepository).existsById(1L);
        verify(productRecipeRepository).findByProductId(1L);
    }

    @Test
    @DisplayName("查詢商品配方清單 - 空清單")
    void listRecipesByProductId_EmptyList() {
        // Given
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRecipeRepository.findByProductId(1L)).thenReturn(Collections.emptyList());

        // When
        List<ProductRecipeDTO> result = productRecipeService.listRecipesByProductId(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("查詢商品配方清單 - 商品不存在拋出例外")
    void listRecipesByProductId_ProductNotFound_ThrowsException() {
        // Given
        when(productRepository.existsById(anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> productRecipeService.listRecipesByProductId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("商品不存在");

        verify(productRecipeRepository, never()).findByProductId(anyLong());
    }

    // ===== listRecipesByMaterialId 測試 =====

    @Test
    @DisplayName("查詢原物料使用清單 - 成功")
    void listRecipesByMaterialId_Success() {
        // Given
        when(materialRepository.existsById(1L)).thenReturn(true);
        when(productRecipeRepository.findByMaterialId(1L)).thenReturn(List.of(testRecipe));

        // When
        List<ProductRecipeDTO> result = productRecipeService.listRecipesByMaterialId(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMaterialId()).isEqualTo(1L);

        verify(materialRepository).existsById(1L);
        verify(productRecipeRepository).findByMaterialId(1L);
    }

    @Test
    @DisplayName("查詢原物料使用清單 - 原物料不存在拋出例外")
    void listRecipesByMaterialId_MaterialNotFound_ThrowsException() {
        // Given
        when(materialRepository.existsById(anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> productRecipeService.listRecipesByMaterialId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("原物料不存在");

        verify(productRecipeRepository, never()).findByMaterialId(anyLong());
    }
}
