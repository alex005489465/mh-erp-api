package com.morningharvest.erp.supplier.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.common.test.TestDataFactory;
import com.morningharvest.erp.supplier.constant.PaymentTerms;
import com.morningharvest.erp.supplier.dto.CreateSupplierRequest;
import com.morningharvest.erp.supplier.dto.SupplierDTO;
import com.morningharvest.erp.supplier.dto.UpdateSupplierRequest;
import com.morningharvest.erp.supplier.entity.Supplier;
import com.morningharvest.erp.supplier.repository.SupplierRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SupplierService 單元測試
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierService 單元測試")
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierService supplierService;

    private Supplier testSupplier;
    private CreateSupplierRequest createRequest;
    private UpdateSupplierRequest updateRequest;

    @BeforeEach
    void setUp() {
        // 初始化測試資料
        testSupplier = TestDataFactory.defaultSupplier()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateSupplierRequest.builder()
                .code("S001")
                .name("測試供應商")
                .shortName("測試")
                .contactPerson("王小明")
                .phone("02-12345678")
                .email("test@example.com")
                .paymentTerms(PaymentTerms.NET30)
                .build();

        updateRequest = UpdateSupplierRequest.builder()
                .id(1L)
                .code("S001")
                .name("更新後供應商")
                .shortName("更新")
                .contactPerson("李小華")
                .phone("02-87654321")
                .paymentTerms(PaymentTerms.NET60)
                .build();
    }

    // ===== 建立供應商測試 =====

    @Test
    @DisplayName("建立供應商 - 成功")
    void createSupplier_Success() {
        // Given
        when(supplierRepository.existsByCode(anyString())).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier s = invocation.getArgument(0);
            s.setId(1L);
            s.setCreatedAt(LocalDateTime.now());
            s.setUpdatedAt(LocalDateTime.now());
            return s;
        });

        // When
        SupplierDTO result = supplierService.createSupplier(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(createRequest.getCode());
        assertThat(result.getName()).isEqualTo(createRequest.getName());
        assertThat(result.getIsActive()).isTrue();
        verify(supplierRepository).existsByCode(createRequest.getCode());
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    @DisplayName("建立供應商 - 編號重複拋出例外")
    void createSupplier_DuplicateCode_ThrowsException() {
        // Given
        when(supplierRepository.existsByCode(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> supplierService.createSupplier(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("供應商編號已存在");

        verify(supplierRepository).existsByCode(createRequest.getCode());
        verify(supplierRepository, never()).save(any());
    }

    // ===== 更新供應商測試 =====

    @Test
    @DisplayName("更新供應商 - 成功")
    void updateSupplier_Success() {
        // Given
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.existsByCodeAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SupplierDTO result = supplierService.updateSupplier(updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(updateRequest.getName());
        assertThat(result.getContactPerson()).isEqualTo(updateRequest.getContactPerson());
        verify(supplierRepository).findById(1L);
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    @DisplayName("更新供應商 - 供應商不存在拋出例外")
    void updateSupplier_NotFound_ThrowsException() {
        // Given
        when(supplierRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> supplierService.updateSupplier(updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("供應商不存在");

        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新供應商 - 編號重複拋出例外")
    void updateSupplier_DuplicateCode_ThrowsException() {
        // Given
        UpdateSupplierRequest requestWithNewCode = UpdateSupplierRequest.builder()
                .id(1L)
                .code("S999")
                .name("更新供應商")
                .build();

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.existsByCodeAndIdNot("S999", 1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> supplierService.updateSupplier(requestWithNewCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("供應商編號已存在");

        verify(supplierRepository, never()).save(any());
    }

    // ===== 查詢供應商測試 =====

    @Test
    @DisplayName("查詢供應商詳情 - 成功")
    void getSupplierById_Success() {
        // Given
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));

        // When
        SupplierDTO result = supplierService.getSupplierById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCode()).isEqualTo(testSupplier.getCode());
        assertThat(result.getPaymentTermsDisplayName()).isEqualTo("30 天付款");
        verify(supplierRepository).findById(1L);
    }

    @Test
    @DisplayName("查詢供應商詳情 - 供應商不存在拋出例外")
    void getSupplierById_NotFound_ThrowsException() {
        // Given
        when(supplierRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> supplierService.getSupplierById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("供應商不存在");
    }

    @Test
    @DisplayName("分頁查詢供應商列表 - 成功")
    void listSuppliers_Success() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Supplier> supplierPage = new PageImpl<>(
                List.of(testSupplier),
                pageableRequest.toPageable(),
                1
        );

        when(supplierRepository.findByKeywordAndIsActive(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(supplierPage);

        // When
        PageResponse<SupplierDTO> result = supplierService.listSuppliers(pageableRequest, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("分頁查詢供應商列表 - 關鍵字搜尋成功")
    void listSuppliers_WithKeyword_Success() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<Supplier> supplierPage = new PageImpl<>(
                List.of(testSupplier),
                pageableRequest.toPageable(),
                1
        );

        when(supplierRepository.findByKeywordAndIsActive(eq("測試"), isNull(), any(Pageable.class)))
                .thenReturn(supplierPage);

        // When
        PageResponse<SupplierDTO> result = supplierService.listSuppliers(pageableRequest, "測試", null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(supplierRepository).findByKeywordAndIsActive(eq("測試"), isNull(), any(Pageable.class));
    }

    // ===== 刪除供應商測試 =====

    @Test
    @DisplayName("刪除供應商 - 成功")
    void deleteSupplier_Success() {
        // Given
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        doNothing().when(supplierRepository).delete(any(Supplier.class));

        // When
        supplierService.deleteSupplier(1L);

        // Then
        verify(supplierRepository).findById(1L);
        verify(supplierRepository).delete(testSupplier);
    }

    @Test
    @DisplayName("刪除供應商 - 供應商不存在拋出例外")
    void deleteSupplier_NotFound_ThrowsException() {
        // Given
        when(supplierRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> supplierService.deleteSupplier(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("供應商不存在");

        verify(supplierRepository, never()).delete(any());
    }

    // ===== 啟用/停用供應商測試 =====

    @Test
    @DisplayName("啟用供應商 - 成功")
    void activateSupplier_Success() {
        // Given
        Supplier inactiveSupplier = TestDataFactory.inactiveSupplier()
                .id(2L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(supplierRepository.findById(2L)).thenReturn(Optional.of(inactiveSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SupplierDTO result = supplierService.activateSupplier(2L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isTrue();
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    @DisplayName("停用供應商 - 成功")
    void deactivateSupplier_Success() {
        // Given
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SupplierDTO result = supplierService.deactivateSupplier(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isFalse();
        verify(supplierRepository).save(any(Supplier.class));
    }
}
