package com.morningharvest.erp.purchase.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.event.EventPublisher;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.common.test.TestDataFactory;
import com.morningharvest.erp.material.entity.Material;
import com.morningharvest.erp.material.repository.MaterialRepository;
import com.morningharvest.erp.purchase.dto.*;
import com.morningharvest.erp.purchase.entity.Purchase;
import com.morningharvest.erp.purchase.entity.PurchaseItem;
import com.morningharvest.erp.purchase.event.PurchaseConfirmedEvent;
import com.morningharvest.erp.purchase.repository.PurchaseItemRepository;
import com.morningharvest.erp.purchase.repository.PurchaseRepository;
import com.morningharvest.erp.supplier.entity.Supplier;
import com.morningharvest.erp.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseService 單元測試")
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private PurchaseItemRepository purchaseItemRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private PurchaseService purchaseService;

    private Purchase testPurchase;
    private Purchase confirmedPurchase;
    private PurchaseItem testPurchaseItem;
    private Supplier testSupplier;
    private Supplier inactiveSupplier;
    private Material testMaterial;
    private Material inactiveMaterial;
    private PageableRequest pageableRequest;

    @BeforeEach
    void setUp() {
        testSupplier = TestDataFactory.defaultSupplier()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        inactiveSupplier = TestDataFactory.inactiveSupplier()
                .id(2L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testMaterial = TestDataFactory.defaultMaterial()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        inactiveMaterial = TestDataFactory.inactiveMaterial()
                .id(2L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testPurchase = TestDataFactory.defaultPurchase()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        confirmedPurchase = TestDataFactory.confirmedPurchase()
                .id(2L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testPurchaseItem = TestDataFactory.defaultPurchaseItem()
                .id(1L)
                .purchaseId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();
    }

    // ===== createPurchase 測試 =====

    @Nested
    @DisplayName("createPurchase 方法測試")
    class CreatePurchaseTests {

        @Test
        @DisplayName("建立進貨單 - 成功")
        void createPurchase_Success() {
            // Given
            CreatePurchaseRequest request = TestDataFactory.defaultCreatePurchaseRequest().build();

            when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            when(purchaseRepository.countByPurchaseNumberPrefix(anyString())).thenReturn(0L);
            when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {
                Purchase p = invocation.getArgument(0);
                p.setId(1L);
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                return p;
            });
            when(purchaseRepository.findById(1L)).thenReturn(Optional.of(testPurchase));
            when(purchaseItemRepository.save(any(PurchaseItem.class))).thenAnswer(invocation -> {
                PurchaseItem item = invocation.getArgument(0);
                item.setId(1L);
                item.setCreatedAt(LocalDateTime.now());
                item.setUpdatedAt(LocalDateTime.now());
                return item;
            });
            when(purchaseItemRepository.findByPurchaseIdOrderByIdAsc(1L))
                    .thenReturn(List.of(testPurchaseItem));

            // When
            PurchaseDetailDTO result = purchaseService.createPurchase(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("DRAFT");
            assertThat(result.getSupplierName()).isEqualTo(testSupplier.getName());
            verify(purchaseRepository, times(2)).save(any(Purchase.class));
            verify(purchaseItemRepository).save(any(PurchaseItem.class));
        }

        @Test
        @DisplayName("建立進貨單 - 供應商不存在")
        void createPurchase_SupplierNotFound() {
            // Given
            CreatePurchaseRequest request = TestDataFactory.defaultCreatePurchaseRequest().build();
            when(supplierRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> purchaseService.createPurchase(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("供應商不存在");

            verify(purchaseRepository, never()).save(any());
        }

        @Test
        @DisplayName("建立進貨單 - 供應商已停用")
        void createPurchase_SupplierInactive() {
            // Given
            CreatePurchaseRequest request = TestDataFactory.defaultCreatePurchaseRequest()
                    .supplierId(2L)
                    .build();
            when(supplierRepository.findById(2L)).thenReturn(Optional.of(inactiveSupplier));

            // When & Then
            assertThatThrownBy(() -> purchaseService.createPurchase(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("供應商已停用");

            verify(purchaseRepository, never()).save(any());
        }

        @Test
        @DisplayName("建立進貨單 - 原物料不存在")
        void createPurchase_MaterialNotFound() {
            // Given
            CreatePurchaseRequest request = TestDataFactory.defaultCreatePurchaseRequest().build();
            when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
            when(purchaseRepository.countByPurchaseNumberPrefix(anyString())).thenReturn(0L);
            when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {
                Purchase p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(materialRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> purchaseService.createPurchase(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("原物料不存在");
        }

        @Test
        @DisplayName("建立進貨單 - 原物料已停用")
        void createPurchase_MaterialInactive() {
            // Given
            CreatePurchaseItemRequest itemRequest = TestDataFactory.defaultCreatePurchaseItemRequest()
                    .materialId(2L)
                    .build();
            CreatePurchaseRequest request = CreatePurchaseRequest.builder()
                    .supplierId(1L)
                    .purchaseDate(LocalDate.now())
                    .items(List.of(itemRequest))
                    .build();

            when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
            when(purchaseRepository.countByPurchaseNumberPrefix(anyString())).thenReturn(0L);
            when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {
                Purchase p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(materialRepository.findById(2L)).thenReturn(Optional.of(inactiveMaterial));

            // When & Then
            assertThatThrownBy(() -> purchaseService.createPurchase(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("原物料已停用");
        }

        @Test
        @DisplayName("建立進貨單 - 無明細項目")
        void createPurchase_NoItems_Success() {
            // Given
            CreatePurchaseRequest request = CreatePurchaseRequest.builder()
                    .supplierId(1L)
                    .purchaseDate(LocalDate.now())
                    .items(Collections.emptyList())
                    .build();

            Purchase emptyPurchase = TestDataFactory.defaultPurchase()
                    .id(1L)
                    .totalAmount(BigDecimal.ZERO)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
            when(purchaseRepository.countByPurchaseNumberPrefix(anyString())).thenReturn(0L);
            when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {
                Purchase p = invocation.getArgument(0);
                p.setId(1L);
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                return p;
            });
            when(purchaseRepository.findById(1L)).thenReturn(Optional.of(emptyPurchase));
            when(purchaseItemRepository.findByPurchaseIdOrderByIdAsc(1L))
                    .thenReturn(Collections.emptyList());

            // When
            PurchaseDetailDTO result = purchaseService.createPurchase(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).isEmpty();
            verify(purchaseItemRepository, never()).save(any());
        }
    }

    // ===== updatePurchase 測試 =====

    @Nested
    @DisplayName("updatePurchase 方法測試")
    class UpdatePurchaseTests {

        @Test
        @DisplayName("更新進貨單 - 成功")
        void updatePurchase_Success() {
            // Given
            UpdatePurchaseRequest request = UpdatePurchaseRequest.builder()
                    .id(1L)
                    .supplierId(1L)
                    .purchaseDate(LocalDate.now())
                    .items(List.of(TestDataFactory.defaultCreatePurchaseItemRequest().build()))
                    .build();

            when(purchaseRepository.findById(1L)).thenReturn(Optional.of(testPurchase));
            when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            doNothing().when(purchaseItemRepository).deleteByPurchaseId(1L);
            when(purchaseRepository.save(any(Purchase.class))).thenReturn(testPurchase);
            when(purchaseItemRepository.save(any(PurchaseItem.class))).thenAnswer(invocation -> {
                PurchaseItem item = invocation.getArgument(0);
                item.setId(1L);
                return item;
            });
            when(purchaseItemRepository.findByPurchaseIdOrderByIdAsc(1L))
                    .thenReturn(List.of(testPurchaseItem));

            // When
            PurchaseDetailDTO result = purchaseService.updatePurchase(request);

            // Then
            assertThat(result).isNotNull();
            verify(purchaseItemRepository).deleteByPurchaseId(1L);
            verify(purchaseRepository, times(2)).save(any(Purchase.class));
        }

        @Test
        @DisplayName("更新進貨單 - 進貨單不存在")
        void updatePurchase_NotFound() {
            // Given
            UpdatePurchaseRequest request = UpdatePurchaseRequest.builder()
                    .id(99L)
                    .supplierId(1L)
                    .build();

            when(purchaseRepository.findById(99L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> purchaseService.updatePurchase(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("進貨單不存在");
        }

        @Test
        @DisplayName("更新進貨單 - 非草稿狀態")
        void updatePurchase_NotDraft() {
            // Given
            UpdatePurchaseRequest request = UpdatePurchaseRequest.builder()
                    .id(2L)
                    .supplierId(1L)
                    .build();

            when(purchaseRepository.findById(2L)).thenReturn(Optional.of(confirmedPurchase));

            // When & Then
            assertThatThrownBy(() -> purchaseService.updatePurchase(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有草稿狀態的進貨單可以操作");
        }
    }

    // ===== deletePurchase 測試 =====

    @Nested
    @DisplayName("deletePurchase 方法測試")
    class DeletePurchaseTests {

        @Test
        @DisplayName("刪除進貨單 - 成功")
        void deletePurchase_Success() {
            // Given
            when(purchaseRepository.findById(1L)).thenReturn(Optional.of(testPurchase));
            doNothing().when(purchaseItemRepository).deleteByPurchaseId(1L);
            doNothing().when(purchaseRepository).delete(testPurchase);

            // When
            purchaseService.deletePurchase(1L);

            // Then
            verify(purchaseItemRepository).deleteByPurchaseId(1L);
            verify(purchaseRepository).delete(testPurchase);
        }

        @Test
        @DisplayName("刪除進貨單 - 進貨單不存在")
        void deletePurchase_NotFound() {
            // Given
            when(purchaseRepository.findById(99L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> purchaseService.deletePurchase(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("進貨單不存在");
        }

        @Test
        @DisplayName("刪除進貨單 - 非草稿狀態")
        void deletePurchase_NotDraft() {
            // Given
            when(purchaseRepository.findById(2L)).thenReturn(Optional.of(confirmedPurchase));

            // When & Then
            assertThatThrownBy(() -> purchaseService.deletePurchase(2L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有草稿狀態的進貨單可以操作");

            verify(purchaseRepository, never()).delete(any());
        }
    }

    // ===== confirmPurchase 測試 =====

    @Nested
    @DisplayName("confirmPurchase 方法測試")
    class ConfirmPurchaseTests {

        @Test
        @DisplayName("確認進貨單 - 成功")
        void confirmPurchase_Success() {
            // Given
            ConfirmPurchaseRequest request = ConfirmPurchaseRequest.builder()
                    .id(1L)
                    .confirmedBy("admin")
                    .build();

            when(purchaseRepository.findById(1L)).thenReturn(Optional.of(testPurchase));
            when(purchaseItemRepository.findByPurchaseIdOrderByIdAsc(1L))
                    .thenReturn(List.of(testPurchaseItem));
            when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {
                Purchase p = invocation.getArgument(0);
                p.setStatus("CONFIRMED");
                p.setConfirmedAt(LocalDateTime.now());
                return p;
            });

            // When
            PurchaseDetailDTO result = purchaseService.confirmPurchase(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
            verify(eventPublisher).publish(any(PurchaseConfirmedEvent.class), eq("進貨確認"));
        }

        @Test
        @DisplayName("確認進貨單 - 無明細項目")
        void confirmPurchase_NoItems() {
            // Given
            ConfirmPurchaseRequest request = ConfirmPurchaseRequest.builder()
                    .id(1L)
                    .confirmedBy("admin")
                    .build();

            when(purchaseRepository.findById(1L)).thenReturn(Optional.of(testPurchase));
            when(purchaseItemRepository.findByPurchaseIdOrderByIdAsc(1L))
                    .thenReturn(Collections.emptyList());

            // When & Then
            assertThatThrownBy(() -> purchaseService.confirmPurchase(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("進貨單沒有明細，無法確認");

            verify(eventPublisher, never()).publish(any(), anyString());
        }

        @Test
        @DisplayName("確認進貨單 - 非草稿狀態")
        void confirmPurchase_NotDraft() {
            // Given
            ConfirmPurchaseRequest request = ConfirmPurchaseRequest.builder()
                    .id(2L)
                    .confirmedBy("admin")
                    .build();

            when(purchaseRepository.findById(2L)).thenReturn(Optional.of(confirmedPurchase));

            // When & Then
            assertThatThrownBy(() -> purchaseService.confirmPurchase(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("只有草稿狀態的進貨單可以操作");

            verify(eventPublisher, never()).publish(any(), anyString());
        }
    }

    // ===== getPurchaseById 測試 =====

    @Nested
    @DisplayName("getPurchaseById 方法測試")
    class GetPurchaseByIdTests {

        @Test
        @DisplayName("查詢進貨單 - 成功")
        void getPurchaseById_Success() {
            // Given
            when(purchaseRepository.findById(1L)).thenReturn(Optional.of(testPurchase));
            when(purchaseItemRepository.findByPurchaseIdOrderByIdAsc(1L))
                    .thenReturn(List.of(testPurchaseItem));

            // When
            PurchaseDetailDTO result = purchaseService.getPurchaseById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("查詢進貨單 - 不存在")
        void getPurchaseById_NotFound() {
            // Given
            when(purchaseRepository.findById(99L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> purchaseService.getPurchaseById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("進貨單不存在");
        }
    }

    // ===== listPurchases 測試 =====

    @Nested
    @DisplayName("listPurchases 方法測試")
    class ListPurchasesTests {

        @Test
        @DisplayName("查詢進貨單列表 - 無篩選條件")
        void listPurchases_NoFilter() {
            // Given
            Page<Purchase> purchasePage = new PageImpl<>(
                    List.of(testPurchase),
                    pageableRequest.toPageable(),
                    1
            );
            when(purchaseRepository.findByFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(purchasePage);

            // When
            PageResponse<PurchaseDTO> result = purchaseService.listPurchases(
                    pageableRequest, null, null, null, null, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("查詢進貨單列表 - 關鍵字篩選")
        void listPurchases_WithKeyword() {
            // Given
            Page<Purchase> purchasePage = new PageImpl<>(
                    List.of(testPurchase),
                    pageableRequest.toPageable(),
                    1
            );
            when(purchaseRepository.findByFilters(eq("PO-"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(purchasePage);

            // When
            PageResponse<PurchaseDTO> result = purchaseService.listPurchases(
                    pageableRequest, "PO-", null, null, null, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("查詢進貨單列表 - 狀態篩選")
        void listPurchases_WithStatus() {
            // Given
            Page<Purchase> purchasePage = new PageImpl<>(
                    List.of(testPurchase),
                    pageableRequest.toPageable(),
                    1
            );
            when(purchaseRepository.findByFilters(isNull(), eq("DRAFT"), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(purchasePage);

            // When
            PageResponse<PurchaseDTO> result = purchaseService.listPurchases(
                    pageableRequest, null, "DRAFT", null, null, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo("DRAFT");
        }

        @Test
        @DisplayName("查詢進貨單列表 - 空結果")
        void listPurchases_EmptyResult() {
            // Given
            Page<Purchase> emptyPage = new PageImpl<>(
                    Collections.emptyList(),
                    pageableRequest.toPageable(),
                    0
            );
            when(purchaseRepository.findByFilters(any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When
            PageResponse<PurchaseDTO> result = purchaseService.listPurchases(
                    pageableRequest, null, null, null, null, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }
}
