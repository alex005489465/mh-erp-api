package com.morningharvest.erp.product.service;

import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.product.dto.*;
import com.morningharvest.erp.product.entity.ProductOptionValue;
import com.morningharvest.erp.product.repository.ProductOptionGroupRepository;
import com.morningharvest.erp.product.repository.ProductOptionValueRepository;
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
@DisplayName("ProductOptionValueService 單元測試")
class ProductOptionValueServiceTest {

    @Mock
    private ProductOptionValueRepository valueRepository;

    @Mock
    private ProductOptionGroupRepository groupRepository;

    @InjectMocks
    private ProductOptionValueService valueService;

    private ProductOptionValue testValue;
    private CreateProductOptionValueRequest createRequest;
    private UpdateProductOptionValueRequest updateRequest;

    @BeforeEach
    void setUp() {
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

        createRequest = CreateProductOptionValueRequest.builder()
                .groupId(1L)
                .name("珍珠")
                .priceAdjustment(new BigDecimal("10.00"))
                .sortOrder(1)
                .build();

        updateRequest = UpdateProductOptionValueRequest.builder()
                .id(1L)
                .name("更新半糖")
                .priceAdjustment(new BigDecimal("5.00"))
                .sortOrder(2)
                .build();
    }

    @Test
    @DisplayName("建立選項值 - 成功")
    void createValue_Success() {
        // Given
        when(groupRepository.existsById(1L)).thenReturn(true);
        when(valueRepository.existsByGroupIdAndName(anyLong(), anyString())).thenReturn(false);
        when(valueRepository.save(any(ProductOptionValue.class))).thenAnswer(invocation -> {
            ProductOptionValue v = invocation.getArgument(0);
            v.setId(2L);
            v.setCreatedAt(LocalDateTime.now());
            v.setUpdatedAt(LocalDateTime.now());
            return v;
        });

        // When
        ProductOptionValueDTO result = valueService.createValue(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(createRequest.getName());
        assertThat(result.getPriceAdjustment()).isEqualByComparingTo(createRequest.getPriceAdjustment());
        verify(groupRepository).existsById(1L);
        verify(valueRepository).save(any(ProductOptionValue.class));
    }

    @Test
    @DisplayName("建立選項值 - 群組不存在拋出例外")
    void createValue_GroupNotFound_ThrowsException() {
        // Given
        when(groupRepository.existsById(anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> valueService.createValue(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("產品選項群組不存在");

        verify(valueRepository, never()).save(any());
    }

    @Test
    @DisplayName("建立選項值 - 同群組內名稱重複拋出例外")
    void createValue_DuplicateName_ThrowsException() {
        // Given
        when(groupRepository.existsById(1L)).thenReturn(true);
        when(valueRepository.existsByGroupIdAndName(anyLong(), anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> valueService.createValue(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("群組內選項名稱已存在");

        verify(valueRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新選項值 - 成功")
    void updateValue_Success() {
        // Given
        when(valueRepository.findById(1L)).thenReturn(Optional.of(testValue));
        when(valueRepository.existsByGroupIdAndNameAndIdNot(anyLong(), anyString(), anyLong())).thenReturn(false);
        when(valueRepository.save(any(ProductOptionValue.class))).thenAnswer(invocation -> {
            ProductOptionValue v = invocation.getArgument(0);
            v.setUpdatedAt(LocalDateTime.now());
            return v;
        });

        // When
        ProductOptionValueDTO result = valueService.updateValue(updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(valueRepository).findById(1L);
        verify(valueRepository).save(any(ProductOptionValue.class));
    }

    @Test
    @DisplayName("更新選項值 - 選項不存在拋出例外")
    void updateValue_NotFound_ThrowsException() {
        // Given
        when(valueRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> valueService.updateValue(updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("產品選項值不存在");

        verify(valueRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新選項值 - 同群組內名稱重複拋出例外")
    void updateValue_DuplicateName_ThrowsException() {
        // Given
        when(valueRepository.findById(1L)).thenReturn(Optional.of(testValue));
        when(valueRepository.existsByGroupIdAndNameAndIdNot(anyLong(), anyString(), anyLong())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> valueService.updateValue(updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("群組內選項名稱已存在");
    }

    @Test
    @DisplayName("刪除選項值 - 成功")
    void deleteValue_Success() {
        // Given
        when(valueRepository.existsById(1L)).thenReturn(true);
        doNothing().when(valueRepository).deleteById(1L);

        // When
        valueService.deleteValue(1L);

        // Then
        verify(valueRepository).existsById(1L);
        verify(valueRepository).deleteById(1L);
    }

    @Test
    @DisplayName("刪除選項值 - 選項不存在拋出例外")
    void deleteValue_NotFound_ThrowsException() {
        // Given
        when(valueRepository.existsById(anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> valueService.deleteValue(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("產品選項值不存在");

        verify(valueRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("查詢群組下的選項值列表 - 成功")
    void listValuesByGroupId_Success() {
        // Given
        when(groupRepository.existsById(1L)).thenReturn(true);
        when(valueRepository.findByGroupIdOrderBySortOrder(1L)).thenReturn(List.of(testValue));

        // When
        List<ProductOptionValueDTO> result = valueService.listValuesByGroupId(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("半糖");
    }

    @Test
    @DisplayName("查詢群組下的選項值 - 群組不存在拋出例外")
    void listValuesByGroupId_GroupNotFound_ThrowsException() {
        // Given
        when(groupRepository.existsById(anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> valueService.listValuesByGroupId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("產品選項群組不存在");
    }

    @Test
    @DisplayName("啟用選項值 - 成功")
    void activateValue_Success() {
        // Given
        testValue.setIsActive(false);
        when(valueRepository.findById(1L)).thenReturn(Optional.of(testValue));
        when(valueRepository.save(any(ProductOptionValue.class))).thenAnswer(invocation -> {
            ProductOptionValue v = invocation.getArgument(0);
            v.setIsActive(true);
            return v;
        });

        // When
        ProductOptionValueDTO result = valueService.activateValue(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("停用選項值 - 成功")
    void deactivateValue_Success() {
        // Given
        testValue.setIsActive(true);
        when(valueRepository.findById(1L)).thenReturn(Optional.of(testValue));
        when(valueRepository.save(any(ProductOptionValue.class))).thenAnswer(invocation -> {
            ProductOptionValue v = invocation.getArgument(0);
            v.setIsActive(false);
            return v;
        });

        // When
        ProductOptionValueDTO result = valueService.deactivateValue(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isFalse();
    }
}
