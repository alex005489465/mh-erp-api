package com.morningharvest.erp.product.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.event.EventPublisher;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.product.dto.CreateProductRequest;
import com.morningharvest.erp.product.dto.ProductDTO;
import com.morningharvest.erp.product.dto.UpdateProductRequest;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.entity.ProductCategory;
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
@DisplayName("ProductService 單元測試")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductCategory testCategory;
    private CreateProductRequest createRequest;
    private UpdateProductRequest updateRequest;

    @BeforeEach
    void setUp() {
        testCategory = ProductCategory.builder()
                .id(1L)
                .name("測試分類")
                .isActive(true)
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("測試商品")
                .description("測試說明")
                .price(new BigDecimal("50.00"))
                .imageUrl("http://example.com/test.jpg")
                .categoryId(null)
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
                .categoryId(null)
                .sortOrder(2)
                .build();

        updateRequest = UpdateProductRequest.builder()
                .id(1L)
                .name("更新商品")
                .description("更新說明")
                .price(new BigDecimal("70.00"))
                .imageUrl("http://example.com/updated.jpg")
                .categoryId(null)
                .sortOrder(3)
                .build();
    }

    @Test
    @DisplayName("建立商品 - 成功（無分類）")
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
        assertThat(result.getCategoryId()).isNull();
        verify(productRepository).existsByName(createRequest.getName());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("建立商品 - 帶分類成功")
    void createProduct_WithCategory_Success() {
        // Given - 前端同時傳入 categoryId 和 categoryName
        createRequest.setCategoryId(1L);
        createRequest.setCategoryName("測試分類");
        when(productRepository.existsByName(anyString())).thenReturn(false);
        when(productCategoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(1L);
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            // categoryName 由前端傳入
            assertThat(p.getCategoryName()).isEqualTo("測試分類");
            return p;
        });

        // When
        ProductDTO result = productService.createProduct(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCategoryId()).isEqualTo(1L);
        assertThat(result.getCategoryName()).isEqualTo("測試分類");
        verify(productCategoryRepository).findById(1L);
    }

    @Test
    @DisplayName("建立商品 - 分類不存在拋出例外")
    void createProduct_CategoryNotFound_ThrowsException() {
        // Given
        createRequest.setCategoryId(999L);
        createRequest.setCategoryName("不存在的分類");
        when(productRepository.existsByName(anyString())).thenReturn(false);
        when(productCategoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("商品分類不存在");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("建立商品 - 分類名稱不一致拋出例外")
    void createProduct_CategoryNameMismatch_ThrowsException() {
        // Given
        createRequest.setCategoryId(1L);
        createRequest.setCategoryName("錯誤的分類名稱");
        when(productRepository.existsByName(anyString())).thenReturn(false);
        when(productCategoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分類名稱不一致");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("建立商品 - 只傳 categoryId 不傳 categoryName 拋出例外")
    void createProduct_OnlyCategoryId_ThrowsException() {
        // Given
        createRequest.setCategoryId(1L);
        createRequest.setCategoryName(null);
        when(productRepository.existsByName(anyString())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分類 ID 與名稱必須同時提供或同時為空");

        verify(productRepository, never()).save(any());
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
    @DisplayName("更新商品 - 變更分類成功")
    void updateProduct_ChangeCategory_Success() {
        // Given - 前端同時傳入 categoryId 和 categoryName
        updateRequest.setCategoryId(1L);
        updateRequest.setCategoryName("測試分類");
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(productCategoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            // 驗證 categoryName 由前端傳入
            assertThat(p.getCategoryName()).isEqualTo("測試分類");
            return p;
        });

        // When
        ProductDTO result = productService.updateProduct(updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCategoryName()).isEqualTo("測試分類");
        verify(productCategoryRepository).findById(1L);
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
        PageResponse<ProductDTO> result = productService.listProducts(pageableRequest, null, null);

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
        PageResponse<ProductDTO> result = productService.listProducts(pageableRequest, true, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).findByIsActive(eq(true), any(Pageable.class));
    }

    @Test
    @DisplayName("分頁查詢商品 - 按分類篩選")
    void listProducts_FilterByCategory() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        testProduct.setCategoryId(1L);
        testProduct.setCategoryName("測試分類");  // 設定冗餘欄位
        Page<Product> productPage = new PageImpl<>(List.of(testProduct));
        when(productRepository.findByCategoryId(eq(1L), any(Pageable.class))).thenReturn(productPage);

        // When
        PageResponse<ProductDTO> result = productService.listProducts(pageableRequest, null, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategoryName()).isEqualTo("測試分類");
        verify(productRepository).findByCategoryId(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("分頁查詢商品 - 按分類和上架狀態篩選")
    void listProducts_FilterByCategoryAndIsActive() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        testProduct.setCategoryId(1L);
        testProduct.setCategoryName("測試分類");  // 設定冗餘欄位
        Page<Product> productPage = new PageImpl<>(List.of(testProduct));
        when(productRepository.findByCategoryIdAndIsActive(eq(1L), eq(true), any(Pageable.class)))
                .thenReturn(productPage);

        // When
        PageResponse<ProductDTO> result = productService.listProducts(pageableRequest, true, 1L);

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).findByCategoryIdAndIsActive(eq(1L), eq(true), any(Pageable.class));
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
