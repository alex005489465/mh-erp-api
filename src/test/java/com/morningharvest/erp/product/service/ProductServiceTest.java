package com.morningharvest.erp.product.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.product.dto.CreateProductRequest;
import com.morningharvest.erp.product.dto.ProductDTO;
import com.morningharvest.erp.product.dto.UpdateProductRequest;
import com.morningharvest.erp.product.entity.Product;
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
@DisplayName("ProductService 單元測試")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private CreateProductRequest createRequest;
    private UpdateProductRequest updateRequest;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(1L)
                .name("測試商品")
                .description("測試說明")
                .price(new BigDecimal("50.00"))
                .imageUrl("http://example.com/test.jpg")
                .isActive(true)
                .sortOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateProductRequest.builder()
                .name("新商品")
                .description("新商品說明")
                .price(new BigDecimal("60.00"))
                .imageUrl("http://example.com/new.jpg")
                .sortOrder(2)
                .build();

        updateRequest = UpdateProductRequest.builder()
                .id(1L)
                .name("更新商品")
                .description("更新說明")
                .price(new BigDecimal("70.00"))
                .imageUrl("http://example.com/updated.jpg")
                .sortOrder(3)
                .build();
    }

    @Test
    @DisplayName("建立商品 - 成功")
    void createProduct_Success() {
        // Given
        when(productRepository.existsByName(anyString())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(1L);
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            return p;
        });

        // When
        ProductDTO result = productService.createProduct(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(createRequest.getName());
        assertThat(result.getPrice()).isEqualByComparingTo(createRequest.getPrice());
        verify(productRepository).existsByName(createRequest.getName());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("建立商品 - 名稱重複拋出例外")
    void createProduct_DuplicateName_ThrowsException() {
        // Given
        when(productRepository.existsByName(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("商品名稱已存在");

        verify(productRepository).existsByName(createRequest.getName());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新商品 - 成功")
    void updateProduct_Success() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        ProductDTO result = productService.updateProduct(updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("更新商品 - 商品不存在拋出例外")
    void updateProduct_NotFound_ThrowsException() {
        // Given
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct(updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("商品不存在");

        verify(productRepository).findById(updateRequest.getId());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新商品 - 名稱與其他商品重複拋出例外")
    void updateProduct_DuplicateName_ThrowsException() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct(updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("商品名稱已存在");
    }

    @Test
    @DisplayName("刪除商品 - 成功")
    void deleteProduct_Success() {
        // Given
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        // When
        productService.deleteProduct(1L);

        // Then
        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    @DisplayName("刪除商品 - 商品不存在拋出例外")
    void deleteProduct_NotFound_ThrowsException() {
        // Given
        when(productRepository.existsById(anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> productService.deleteProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("商品不存在");

        verify(productRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("查詢商品 - 成功")
    void getProductById_Success() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        ProductDTO result = productService.getProductById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testProduct.getId());
        assertThat(result.getName()).isEqualTo(testProduct.getName());
    }

    @Test
    @DisplayName("查詢商品 - 商品不存在拋出例外")
    void getProductById_NotFound_ThrowsException() {
        // Given
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("商品不存在");
    }

    @Test
    @DisplayName("分頁查詢商品列表")
    void listProducts_WithPagination() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Product> productPage = new PageImpl<>(List.of(testProduct));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);

        // When
        PageResponse<ProductDTO> result = productService.listProducts(pageableRequest, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("分頁查詢商品 - 篩選上架狀態")
    void listProducts_FilterByIsActive() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Product> productPage = new PageImpl<>(List.of(testProduct));
        when(productRepository.findByIsActive(eq(true), any(Pageable.class))).thenReturn(productPage);

        // When
        PageResponse<ProductDTO> result = productService.listProducts(pageableRequest, true);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).findByIsActive(eq(true), any(Pageable.class));
    }

    @Test
    @DisplayName("上架商品 - 成功")
    void activateProduct_Success() {
        // Given
        testProduct.setIsActive(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setIsActive(true);
            return p;
        });

        // When
        ProductDTO result = productService.activateProduct(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("下架商品 - 成功")
    void deactivateProduct_Success() {
        // Given
        testProduct.setIsActive(true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setIsActive(false);
            return p;
        });

        // When
        ProductDTO result = productService.deactivateProduct(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isFalse();
    }
}
