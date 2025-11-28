package com.morningharvest.erp.combo.service;

import com.morningharvest.erp.combo.dto.*;
import com.morningharvest.erp.combo.entity.ComboItem;
import com.morningharvest.erp.combo.repository.ComboItemRepository;
import com.morningharvest.erp.combo.repository.ComboRepository;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.product.entity.Product;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComboItemService 單元測試")
class ComboItemServiceTest {

    @Mock
    private ComboItemRepository comboItemRepository;

    @Mock
    private ComboRepository comboRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ComboItemService comboItemService;

    private ComboItem testItem;
    private Product testProduct;
    private CreateComboItemRequest createRequest;
    private UpdateComboItemRequest updateRequest;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(100L)
                .name("招牌漢堡")
                .price(new BigDecimal("59.00"))
                .isActive(true)
                .build();

        testItem = ComboItem.builder()
                .id(1L)
                .comboId(1L)
                .productId(100L)
                .productName("招牌漢堡")
                .quantity(1)
                .sortOrder(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateComboItemRequest.builder()
                .comboId(1L)
                .productId(100L)
                .quantity(1)
                .sortOrder(0)
                .build();

        updateRequest = UpdateComboItemRequest.builder()
                .productId(200L)
                .quantity(2)
                .sortOrder(1)
                .build();
    }

    @Test
    @DisplayName("建立套餐項目 - 成功")
    void createComboItem_Success() {
        // Given
        when(comboRepository.existsById(1L)).thenReturn(true);
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(comboItemRepository.existsByComboIdAndProductId(1L, 100L)).thenReturn(false);
        when(comboItemRepository.save(any(ComboItem.class))).thenAnswer(invocation -> {
            ComboItem item = invocation.getArgument(0);
            item.setId(1L);
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            return item;
        });

        // When
        ComboItemDTO result = comboItemService.createComboItem(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getComboId()).isEqualTo(1L);
        assertThat(result.getProductId()).isEqualTo(100L);
        assertThat(result.getProductName()).isEqualTo("招牌漢堡");
        assertThat(result.getQuantity()).isEqualTo(1);
        verify(comboRepository).existsById(1L);
        verify(productRepository).findById(100L);
        verify(comboItemRepository).save(any(ComboItem.class));
    }

    @Test
    @DisplayName("建立套餐項目 - 套餐不存在拋出例外")
    void createComboItem_ComboNotFound_ThrowsException() {
        // Given
        when(comboRepository.existsById(anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> comboItemService.createComboItem(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("套餐不存在");

        verify(comboRepository).existsById(1L);
        verify(productRepository, never()).findById(anyLong());
        verify(comboItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("建立套餐項目 - 商品不存在拋出例外")
    void createComboItem_ProductNotFound_ThrowsException() {
        // Given
        when(comboRepository.existsById(1L)).thenReturn(true);
        when(productRepository.findById(100L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> comboItemService.createComboItem(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("商品不存在");

        verify(comboRepository).existsById(1L);
        verify(productRepository).findById(100L);
        verify(comboItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("建立套餐項目 - 商品已存在於套餐中拋出例外")
    void createComboItem_DuplicateProduct_ThrowsException() {
        // Given
        when(comboRepository.existsById(1L)).thenReturn(true);
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(comboItemRepository.existsByComboIdAndProductId(1L, 100L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> comboItemService.createComboItem(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("此套餐已包含此商品");

        verify(comboItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("批次建立套餐項目 - 成功")
    void batchCreateComboItems_Success() {
        // Given
        Product product1 = Product.builder().id(100L).name("招牌漢堡").build();
        Product product2 = Product.builder().id(200L).name("薯條").build();

        BatchCreateComboItemRequest request = BatchCreateComboItemRequest.builder()
                .comboId(1L)
                .items(List.of(
                        BatchCreateComboItemRequest.ComboItemInput.builder()
                                .productId(100L)
                                .quantity(1)
                                .sortOrder(0)
                                .build(),
                        BatchCreateComboItemRequest.ComboItemInput.builder()
                                .productId(200L)
                                .quantity(1)
                                .sortOrder(1)
                                .build()
                ))
                .build();

        when(comboRepository.existsById(1L)).thenReturn(true);
        when(productRepository.findById(100L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(200L)).thenReturn(Optional.of(product2));
        when(comboItemRepository.existsByComboIdAndProductId(eq(1L), anyLong())).thenReturn(false);
        when(comboItemRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<ComboItem> items = invocation.getArgument(0);
            long id = 1L;
            for (ComboItem item : items) {
                item.setId(id++);
                item.setCreatedAt(LocalDateTime.now());
                item.setUpdatedAt(LocalDateTime.now());
            }
            return items;
        });

        // When
        List<ComboItemDTO> result = comboItemService.batchCreateComboItems(request);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductName()).isEqualTo("招牌漢堡");
        assertThat(result.get(1).getProductName()).isEqualTo("薯條");
        verify(comboItemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("批次建立套餐項目 - 套餐不存在拋出例外")
    void batchCreateComboItems_ComboNotFound_ThrowsException() {
        // Given
        BatchCreateComboItemRequest request = BatchCreateComboItemRequest.builder()
                .comboId(999L)
                .items(List.of(
                        BatchCreateComboItemRequest.ComboItemInput.builder()
                                .productId(100L)
                                .quantity(1)
                                .build()
                ))
                .build();

        when(comboRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> comboItemService.batchCreateComboItems(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("套餐不存在");

        verify(comboItemRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("批次建立套餐項目 - 部分商品不存在拋出例外")
    void batchCreateComboItems_SomeProductNotFound_ThrowsException() {
        // Given
        Product product1 = Product.builder().id(100L).name("招牌漢堡").build();

        BatchCreateComboItemRequest request = BatchCreateComboItemRequest.builder()
                .comboId(1L)
                .items(List.of(
                        BatchCreateComboItemRequest.ComboItemInput.builder()
                                .productId(100L)
                                .quantity(1)
                                .build(),
                        BatchCreateComboItemRequest.ComboItemInput.builder()
                                .productId(999L)
                                .quantity(1)
                                .build()
                ))
                .build();

        when(comboRepository.existsById(1L)).thenReturn(true);
        when(productRepository.findById(100L)).thenReturn(Optional.of(product1));
        when(comboItemRepository.existsByComboIdAndProductId(1L, 100L)).thenReturn(false);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> comboItemService.batchCreateComboItems(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("商品不存在");

        verify(comboItemRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("更新套餐項目 - 成功")
    void updateComboItem_Success() {
        // Given
        Product newProduct = Product.builder().id(200L).name("雞塊").build();

        when(comboItemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(productRepository.findById(200L)).thenReturn(Optional.of(newProduct));
        when(comboItemRepository.existsByComboIdAndProductIdAndIdNot(1L, 200L, 1L)).thenReturn(false);
        when(comboItemRepository.save(any(ComboItem.class))).thenReturn(testItem);

        // When
        ComboItemDTO result = comboItemService.updateComboItem(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(comboItemRepository).findById(1L);
        verify(comboItemRepository).save(any(ComboItem.class));
    }

    @Test
    @DisplayName("更新套餐項目 - 項目不存在拋出例外")
    void updateComboItem_NotFound_ThrowsException() {
        // Given
        when(comboItemRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> comboItemService.updateComboItem(999L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("套餐項目不存在");

        verify(comboItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新套餐項目 - 更換商品但商品不存在拋出例外")
    void updateComboItem_NewProductNotFound_ThrowsException() {
        // Given
        when(comboItemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(productRepository.findById(200L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> comboItemService.updateComboItem(1L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("商品不存在");

        verify(comboItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新套餐項目 - 更換商品但已存在於套餐中拋出例外")
    void updateComboItem_ChangeToExistingProduct_ThrowsException() {
        // Given
        Product newProduct = Product.builder().id(200L).name("雞塊").build();

        when(comboItemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(productRepository.findById(200L)).thenReturn(Optional.of(newProduct));
        when(comboItemRepository.existsByComboIdAndProductIdAndIdNot(1L, 200L, 1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> comboItemService.updateComboItem(1L, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("此套餐已包含此商品");

        verify(comboItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("刪除套餐項目 - 成功")
    void deleteComboItem_Success() {
        // Given
        when(comboItemRepository.existsById(1L)).thenReturn(true);
        doNothing().when(comboItemRepository).deleteById(1L);

        // When
        comboItemService.deleteComboItem(1L);

        // Then
        verify(comboItemRepository).existsById(1L);
        verify(comboItemRepository).deleteById(1L);
    }

    @Test
    @DisplayName("刪除套餐項目 - 項目不存在拋出例外")
    void deleteComboItem_NotFound_ThrowsException() {
        // Given
        when(comboItemRepository.existsById(anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> comboItemService.deleteComboItem(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("套餐項目不存在");

        verify(comboItemRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("查詢套餐項目 - 成功")
    void getComboItemById_Success() {
        // Given
        when(comboItemRepository.findById(1L)).thenReturn(Optional.of(testItem));

        // When
        ComboItemDTO result = comboItemService.getComboItemById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProductName()).isEqualTo("招牌漢堡");
    }

    @Test
    @DisplayName("查詢套餐項目 - 項目不存在拋出例外")
    void getComboItemById_NotFound_ThrowsException() {
        // Given
        when(comboItemRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> comboItemService.getComboItemById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("套餐項目不存在");
    }

    @Test
    @DisplayName("查詢套餐項目列表 - 成功")
    void listComboItemsByComboId_Success() {
        // Given
        ComboItem item2 = ComboItem.builder()
                .id(2L)
                .comboId(1L)
                .productId(200L)
                .productName("薯條")
                .quantity(1)
                .sortOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(comboRepository.existsById(1L)).thenReturn(true);
        when(comboItemRepository.findByComboIdOrderBySortOrder(1L)).thenReturn(List.of(testItem, item2));

        // When
        List<ComboItemDTO> result = comboItemService.listComboItemsByComboId(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductName()).isEqualTo("招牌漢堡");
        assertThat(result.get(1).getProductName()).isEqualTo("薯條");
        verify(comboRepository).existsById(1L);
        verify(comboItemRepository).findByComboIdOrderBySortOrder(1L);
    }

    @Test
    @DisplayName("查詢套餐項目列表 - 套餐不存在拋出例外")
    void listComboItemsByComboId_ComboNotFound_ThrowsException() {
        // Given
        when(comboRepository.existsById(anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> comboItemService.listComboItemsByComboId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("套餐不存在");

        verify(comboItemRepository, never()).findByComboIdOrderBySortOrder(anyLong());
    }
}
