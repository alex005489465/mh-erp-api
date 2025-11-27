package com.morningharvest.erp.product.listener;

import com.morningharvest.erp.product.dto.ProductCategoryDTO;
import com.morningharvest.erp.product.event.ProductCategoryUpdatedEvent;
import com.morningharvest.erp.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductCategoryEventListener 單元測試")
class ProductCategoryEventListenerTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductCategoryEventListener listener;

    private ProductCategoryDTO beforeDTO;
    private ProductCategoryDTO afterDTO;

    @BeforeEach
    void setUp() {
        beforeDTO = ProductCategoryDTO.builder()
                .id(1L)
                .name("舊名稱")
                .description("說明")
                .sortOrder(1)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        afterDTO = ProductCategoryDTO.builder()
                .id(1L)
                .name("新名稱")
                .description("說明")
                .sortOrder(1)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("名稱變更時 - 應批量更新商品")
    void onCategoryUpdated_NameChanged_ShouldUpdateProducts() {
        // Given
        ProductCategoryUpdatedEvent event = new ProductCategoryUpdatedEvent(beforeDTO, afterDTO);
        when(productRepository.updateCategoryNameByCategoryId(1L, "新名稱")).thenReturn(5);

        // When
        listener.onCategoryUpdated(event);

        // Then
        verify(productRepository).updateCategoryNameByCategoryId(1L, "新名稱");
    }

    @Test
    @DisplayName("名稱未變更時 - 不應更新商品")
    void onCategoryUpdated_NameNotChanged_ShouldNotUpdateProducts() {
        // Given
        afterDTO = ProductCategoryDTO.builder()
                .id(1L)
                .name("舊名稱")  // 名稱相同
                .description("更新後說明")
                .sortOrder(2)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ProductCategoryUpdatedEvent event = new ProductCategoryUpdatedEvent(beforeDTO, afterDTO);

        // When
        listener.onCategoryUpdated(event);

        // Then
        verify(productRepository, never()).updateCategoryNameByCategoryId(anyLong(), anyString());
    }

    @Test
    @DisplayName("該分類無商品時 - 更新計數為 0")
    void onCategoryUpdated_NoProducts_ShouldReturnZero() {
        // Given
        ProductCategoryUpdatedEvent event = new ProductCategoryUpdatedEvent(beforeDTO, afterDTO);
        when(productRepository.updateCategoryNameByCategoryId(1L, "新名稱")).thenReturn(0);

        // When
        listener.onCategoryUpdated(event);

        // Then
        verify(productRepository).updateCategoryNameByCategoryId(1L, "新名稱");
    }
}
