package com.morningharvest.erp.inventorycheck.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.event.EventPublisher;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.common.test.TestDataFactory;
import com.morningharvest.erp.inventorycheck.constant.InventoryCheckStatus;
import com.morningharvest.erp.inventorycheck.dto.*;
import com.morningharvest.erp.inventorycheck.entity.InventoryCheck;
import com.morningharvest.erp.inventorycheck.entity.InventoryCheckItem;
import com.morningharvest.erp.inventorycheck.event.InventoryCheckConfirmedEvent;
import com.morningharvest.erp.inventorycheck.repository.InventoryCheckItemRepository;
import com.morningharvest.erp.inventorycheck.repository.InventoryCheckRepository;
import com.morningharvest.erp.material.entity.Material;
import com.morningharvest.erp.material.repository.MaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryCheckService 單元測試")
class InventoryCheckServiceTest {

    @Mock
    private InventoryCheckRepository inventoryCheckRepository;

    @Mock
    private InventoryCheckItemRepository inventoryCheckItemRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private InventoryCheckService inventoryCheckService;

    private InventoryCheck testInventoryCheck;
    private InventoryCheckItem testInventoryCheckItem;
    private Material testMaterial;

    @BeforeEach
    void setUp() {
        testInventoryCheck = TestDataFactory.defaultInventoryCheck()
                .id(1L)
                .build();

        testInventoryCheckItem = TestDataFactory.defaultInventoryCheckItem()
                .id(1L)
                .inventoryCheckId(1L)
                .build();

        testMaterial = TestDataFactory.defaultMaterial()
                .id(1L)
                .build();
    }

    @Nested
    @DisplayName("createInventoryCheck 方法測試")
    class CreateInventoryCheckTests {

        @Test
        @DisplayName("成功建立盤點計畫")
        void createInventoryCheck_Success() {
            // Given
            CreateInventoryCheckRequest request = CreateInventoryCheckRequest.builder()
                    .checkDate(LocalDate.now())
                    .note("測試盤點")
                    .build();

            Page<Material> materialPage = new PageImpl<>(List.of(testMaterial));
            when(materialRepository.findByIsActive(eq(true), any(Pageable.class)))
                    .thenReturn(materialPage);

            when(inventoryCheckRepository.countByCheckNumberPrefix(anyString())).thenReturn(0L);
            when(inventoryCheckRepository.save(any(InventoryCheck.class))).thenAnswer(invocation -> {
                InventoryCheck ic = invocation.getArgument(0);
                ic.setId(1L);
                return ic;
            });
            when(inventoryCheckItemRepository.save(any(InventoryCheckItem.class))).thenAnswer(invocation -> {
                InventoryCheckItem item = invocation.getArgument(0);
                item.setId(1L);
                return item;
            });
            when(inventoryCheckRepository.findById(1L)).thenReturn(Optional.of(testInventoryCheck));
            when(inventoryCheckItemRepository.findByInventoryCheckIdOrderByIdAsc(1L))
                    .thenReturn(List.of(testInventoryCheckItem));
            when(inventoryCheckItemRepository.countByInventoryCheckIdAndIsCheckedTrue(1L)).thenReturn(0L);
            when(inventoryCheckItemRepository.countByInventoryCheckIdAndIsCheckedFalse(1L)).thenReturn(1L);

            // When
            InventoryCheckDetailDTO result = inventoryCheckService.createInventoryCheck(request);

            // Then
            assertThat(result).isNotNull();
            verify(inventoryCheckRepository, times(2)).save(any(InventoryCheck.class));
            verify(inventoryCheckItemRepository).save(any(InventoryCheckItem.class));
        }

        @Test
        @DisplayName("無啟用原物料時建立空盤點單")
        void createInventoryCheck_NoActiveMaterials() {
            // Given
            CreateInventoryCheckRequest request = CreateInventoryCheckRequest.builder()
                    .checkDate(LocalDate.now())
                    .build();

            Page<Material> emptyPage = new PageImpl<>(Collections.emptyList());
            when(materialRepository.findByIsActive(eq(true), any(Pageable.class)))
                    .thenReturn(emptyPage);

            when(inventoryCheckRepository.countByCheckNumberPrefix(anyString())).thenReturn(0L);
            when(inventoryCheckRepository.save(any(InventoryCheck.class))).thenAnswer(invocation -> {
                InventoryCheck ic = invocation.getArgument(0);
                ic.setId(1L);
                ic.setTotalItems(0);
                return ic;
            });
            when(inventoryCheckRepository.findById(1L)).thenReturn(Optional.of(testInventoryCheck));
            when(inventoryCheckItemRepository.findByInventoryCheckIdOrderByIdAsc(1L))
                    .thenReturn(Collections.emptyList());
            when(inventoryCheckItemRepository.countByInventoryCheckIdAndIsCheckedTrue(1L)).thenReturn(0L);
            when(inventoryCheckItemRepository.countByInventoryCheckIdAndIsCheckedFalse(1L)).thenReturn(0L);

            // When
            InventoryCheckDetailDTO result = inventoryCheckService.createInventoryCheck(request);

            // Then
            assertThat(result).isNotNull();
            verify(inventoryCheckItemRepository, never()).save(any(InventoryCheckItem.class));
        }
    }

    @Nested
    @DisplayName("startInventoryCheck 方法測試")
    class StartInventoryCheckTests {

        @Test
        @DisplayName("成功開始盤點")
        void startInventoryCheck_Success() {
            // Given
            StartInventoryCheckRequest request = StartInventoryCheckRequest.builder()
                    .id(1L)
                    .startedBy("admin")
                    .build();

            when(inventoryCheckRepository.findById(1L)).thenReturn(Optional.of(testInventoryCheck));
            when(inventoryCheckItemRepository.existsByInventoryCheckId(1L)).thenReturn(true);
            when(inventoryCheckRepository.save(any(InventoryCheck.class))).thenReturn(testInventoryCheck);
            when(inventoryCheckItemRepository.findByInventoryCheckIdOrderByIdAsc(1L))
                    .thenReturn(List.of(testInventoryCheckItem));
            when(inventoryCheckItemRepository.countByInventoryCheckIdAndIsCheckedTrue(1L)).thenReturn(0L);
            when(inventoryCheckItemRepository.countByInventoryCheckIdAndIsCheckedFalse(1L)).thenReturn(1L);

            // When
            InventoryCheckDetailDTO result = inventoryCheckService.startInventoryCheck(request);

            // Then
            assertThat(result).isNotNull();
            ArgumentCaptor<InventoryCheck> captor = ArgumentCaptor.forClass(InventoryCheck.class);
            verify(inventoryCheckRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(InventoryCheckStatus.IN_PROGRESS);
            assertThat(captor.getValue().getStartedBy()).isEqualTo("admin");
        }

        @Test
        @DisplayName("盤點單不存在拋出例外")
        void startInventoryCheck_NotFound() {
            // Given
            StartInventoryCheckRequest request = StartInventoryCheckRequest.builder()
                    .id(999L)
                    .build();

            when(inventoryCheckRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> inventoryCheckService.startInventoryCheck(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("盤點單不存在");
        }

        @Test
        @DisplayName("狀態非 PLANNED 拋出例外")
        void startInventoryCheck_InvalidStatus() {
            // Given
            StartInventoryCheckRequest request = StartInventoryCheckRequest.builder()
                    .id(1L)
                    .build();

            InventoryCheck inProgressCheck = TestDataFactory.inProgressInventoryCheck()
                    .id(1L)
                    .build();

            when(inventoryCheckRepository.findById(1L)).thenReturn(Optional.of(inProgressCheck));

            // When & Then
            assertThatThrownBy(() -> inventoryCheckService.startInventoryCheck(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("計畫中狀態");
        }

        @Test
        @DisplayName("無盤點項目拋出例外")
        void startInventoryCheck_NoItems() {
            // Given
            StartInventoryCheckRequest request = StartInventoryCheckRequest.builder()
                    .id(1L)
                    .build();

            when(inventoryCheckRepository.findById(1L)).thenReturn(Optional.of(testInventoryCheck));
            when(inventoryCheckItemRepository.existsByInventoryCheckId(1L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> inventoryCheckService.startInventoryCheck(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("沒有項目");
        }
    }

    @Nested
    @DisplayName("updateInventoryCheckItem 方法測試")
    class UpdateInventoryCheckItemTests {

        @Test
        @DisplayName("成功更新盤點數量")
        void updateInventoryCheckItem_Success() {
            // Given
            UpdateInventoryCheckItemRequest request = UpdateInventoryCheckItemRequest.builder()
                    .itemId(1L)
                    .actualQuantity(new BigDecimal("48.00"))
                    .note("實際盤點")
                    .build();

            InventoryCheck inProgressCheck = TestDataFactory.inProgressInventoryCheck()
                    .id(1L)
                    .build();

            when(inventoryCheckItemRepository.findById(1L)).thenReturn(Optional.of(testInventoryCheckItem));
            when(inventoryCheckRepository.findById(1L)).thenReturn(Optional.of(inProgressCheck));
            when(inventoryCheckItemRepository.save(any(InventoryCheckItem.class))).thenReturn(testInventoryCheckItem);
            when(inventoryCheckItemRepository.findByInventoryCheckIdOrderByIdAsc(1L))
                    .thenReturn(List.of(testInventoryCheckItem));
            when(inventoryCheckRepository.save(any(InventoryCheck.class))).thenReturn(inProgressCheck);

            // When
            InventoryCheckItemDTO result = inventoryCheckService.updateInventoryCheckItem(request);

            // Then
            assertThat(result).isNotNull();
            ArgumentCaptor<InventoryCheckItem> captor = ArgumentCaptor.forClass(InventoryCheckItem.class);
            verify(inventoryCheckItemRepository).save(captor.capture());
            assertThat(captor.getValue().getActualQuantity()).isEqualByComparingTo(new BigDecimal("48.00"));
            assertThat(captor.getValue().getIsChecked()).isTrue();
        }

        @Test
        @DisplayName("明細不存在拋出例外")
        void updateInventoryCheckItem_ItemNotFound() {
            // Given
            UpdateInventoryCheckItemRequest request = UpdateInventoryCheckItemRequest.builder()
                    .itemId(999L)
                    .actualQuantity(new BigDecimal("48.00"))
                    .build();

            when(inventoryCheckItemRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> inventoryCheckService.updateInventoryCheckItem(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("盤點明細不存在");
        }

        @Test
        @DisplayName("狀態非 IN_PROGRESS 拋出例外")
        void updateInventoryCheckItem_InvalidStatus() {
            // Given
            UpdateInventoryCheckItemRequest request = UpdateInventoryCheckItemRequest.builder()
                    .itemId(1L)
                    .actualQuantity(new BigDecimal("48.00"))
                    .build();

            when(inventoryCheckItemRepository.findById(1L)).thenReturn(Optional.of(testInventoryCheckItem));
            when(inventoryCheckRepository.findById(1L)).thenReturn(Optional.of(testInventoryCheck)); // PLANNED 狀態

            // When & Then
            assertThatThrownBy(() -> inventoryCheckService.updateInventoryCheckItem(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("盤點中狀態");
        }
    }

    @Nested
    @DisplayName("confirmInventoryCheck 方法測試")
    class ConfirmInventoryCheckTests {

        @Test
        @DisplayName("成功確認盤點")
        void confirmInventoryCheck_Success() {
            // Given
            ConfirmInventoryCheckRequest request = ConfirmInventoryCheckRequest.builder()
                    .id(1L)
                    .confirmedBy("admin")
                    .build();

            InventoryCheck inProgressCheck = TestDataFactory.inProgressInventoryCheck()
                    .id(1L)
                    .build();

            InventoryCheckItem checkedItem = TestDataFactory.checkedInventoryCheckItem()
                    .id(1L)
                    .inventoryCheckId(1L)
                    .build();

            when(inventoryCheckRepository.findById(1L)).thenReturn(Optional.of(inProgressCheck));
            when(inventoryCheckItemRepository.countByInventoryCheckIdAndIsCheckedFalse(1L)).thenReturn(0L);
            when(inventoryCheckRepository.save(any(InventoryCheck.class))).thenReturn(inProgressCheck);
            when(inventoryCheckItemRepository.findByInventoryCheckIdOrderByIdAsc(1L))
                    .thenReturn(List.of(checkedItem));
            when(inventoryCheckItemRepository.countByInventoryCheckIdAndIsCheckedTrue(1L)).thenReturn(1L);

            // When
            InventoryCheckDetailDTO result = inventoryCheckService.confirmInventoryCheck(request);

            // Then
            assertThat(result).isNotNull();
            ArgumentCaptor<InventoryCheck> captor = ArgumentCaptor.forClass(InventoryCheck.class);
            verify(inventoryCheckRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(InventoryCheckStatus.CONFIRMED);
            assertThat(captor.getValue().getConfirmedBy()).isEqualTo("admin");

            // 驗證事件發布
            verify(eventPublisher).publish(any(InventoryCheckConfirmedEvent.class), anyString());
        }

        @Test
        @DisplayName("有未盤點項目拋出例外")
        void confirmInventoryCheck_UncheckedItems() {
            // Given
            ConfirmInventoryCheckRequest request = ConfirmInventoryCheckRequest.builder()
                    .id(1L)
                    .build();

            InventoryCheck inProgressCheck = TestDataFactory.inProgressInventoryCheck()
                    .id(1L)
                    .build();

            when(inventoryCheckRepository.findById(1L)).thenReturn(Optional.of(inProgressCheck));
            when(inventoryCheckItemRepository.countByInventoryCheckIdAndIsCheckedFalse(1L)).thenReturn(3L);

            // When & Then
            assertThatThrownBy(() -> inventoryCheckService.confirmInventoryCheck(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("3 個品項未盤點");
        }

        @Test
        @DisplayName("狀態非 IN_PROGRESS 拋出例外")
        void confirmInventoryCheck_InvalidStatus() {
            // Given
            ConfirmInventoryCheckRequest request = ConfirmInventoryCheckRequest.builder()
                    .id(1L)
                    .build();

            when(inventoryCheckRepository.findById(1L)).thenReturn(Optional.of(testInventoryCheck)); // PLANNED 狀態

            // When & Then
            assertThatThrownBy(() -> inventoryCheckService.confirmInventoryCheck(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("盤點中狀態");
        }
    }

    @Nested
    @DisplayName("deleteInventoryCheck 方法測試")
    class DeleteInventoryCheckTests {

        @Test
        @DisplayName("成功刪除盤點單")
        void deleteInventoryCheck_Success() {
            // Given
            when(inventoryCheckRepository.findById(1L)).thenReturn(Optional.of(testInventoryCheck));
            doNothing().when(inventoryCheckItemRepository).deleteByInventoryCheckId(1L);
            doNothing().when(inventoryCheckRepository).delete(testInventoryCheck);

            // When
            inventoryCheckService.deleteInventoryCheck(1L);

            // Then
            verify(inventoryCheckItemRepository).deleteByInventoryCheckId(1L);
            verify(inventoryCheckRepository).delete(testInventoryCheck);
        }

        @Test
        @DisplayName("狀態非 PLANNED 拋出例外")
        void deleteInventoryCheck_InvalidStatus() {
            // Given
            InventoryCheck inProgressCheck = TestDataFactory.inProgressInventoryCheck()
                    .id(1L)
                    .build();

            when(inventoryCheckRepository.findById(1L)).thenReturn(Optional.of(inProgressCheck));

            // When & Then
            assertThatThrownBy(() -> inventoryCheckService.deleteInventoryCheck(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("計畫中狀態");

            verify(inventoryCheckItemRepository, never()).deleteByInventoryCheckId(anyLong());
            verify(inventoryCheckRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("getInventoryCheckById 方法測試")
    class GetInventoryCheckByIdTests {

        @Test
        @DisplayName("成功查詢盤點單詳情")
        void getInventoryCheckById_Success() {
            // Given
            when(inventoryCheckRepository.findById(1L)).thenReturn(Optional.of(testInventoryCheck));
            when(inventoryCheckItemRepository.findByInventoryCheckIdOrderByIdAsc(1L))
                    .thenReturn(List.of(testInventoryCheckItem));
            when(inventoryCheckItemRepository.countByInventoryCheckIdAndIsCheckedTrue(1L)).thenReturn(0L);
            when(inventoryCheckItemRepository.countByInventoryCheckIdAndIsCheckedFalse(1L)).thenReturn(1L);

            // When
            InventoryCheckDetailDTO result = inventoryCheckService.getInventoryCheckById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getUncheckedItems()).isEqualTo(1);
        }

        @Test
        @DisplayName("盤點單不存在拋出例外")
        void getInventoryCheckById_NotFound() {
            // Given
            when(inventoryCheckRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> inventoryCheckService.getInventoryCheckById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("盤點單不存在");
        }
    }

    @Nested
    @DisplayName("listInventoryChecks 方法測試")
    class ListInventoryChecksTests {

        @Test
        @DisplayName("分頁查詢盤點單列表")
        void listInventoryChecks_WithPagination() {
            // Given
            PageableRequest pageableRequest = PageableRequest.builder()
                    .page(1)
                    .size(10)
                    .build();

            Page<InventoryCheck> page = new PageImpl<>(List.of(testInventoryCheck));
            when(inventoryCheckRepository.findByFilters(any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(page);

            // When
            PageResponse<InventoryCheckDTO> result = inventoryCheckService.listInventoryChecks(
                    pageableRequest, null, null, null, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("篩選條件查詢")
        void listInventoryChecks_WithFilters() {
            // Given
            PageableRequest pageableRequest = PageableRequest.builder()
                    .page(1)
                    .size(10)
                    .build();

            Page<InventoryCheck> page = new PageImpl<>(List.of(testInventoryCheck));
            when(inventoryCheckRepository.findByFilters(
                    eq("IC-"),
                    eq(InventoryCheckStatus.PLANNED),
                    eq(LocalDate.now()),
                    eq(LocalDate.now()),
                    any(Pageable.class)))
                    .thenReturn(page);

            // When
            PageResponse<InventoryCheckDTO> result = inventoryCheckService.listInventoryChecks(
                    pageableRequest,
                    "IC-",
                    InventoryCheckStatus.PLANNED,
                    LocalDate.now(),
                    LocalDate.now());

            // Then
            assertThat(result).isNotNull();
            verify(inventoryCheckRepository).findByFilters(
                    eq("IC-"),
                    eq(InventoryCheckStatus.PLANNED),
                    eq(LocalDate.now()),
                    eq(LocalDate.now()),
                    any(Pageable.class));
        }
    }
}
