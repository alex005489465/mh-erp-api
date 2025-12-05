package com.morningharvest.erp.material.listener;

import com.morningharvest.erp.common.test.TestDataFactory;
import com.morningharvest.erp.material.entity.Material;
import com.morningharvest.erp.material.repository.MaterialRepository;
import com.morningharvest.erp.purchase.event.PurchaseConfirmedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaterialStockEventListener 單元測試")
class MaterialStockEventListenerTest {

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private MaterialStockEventListener listener;

    private Material testMaterial;
    private PurchaseConfirmedEvent event;

    @BeforeEach
    void setUp() {
        testMaterial = TestDataFactory.defaultMaterial()
                .id(1L)
                .currentStockQuantity(new BigDecimal("50.00"))
                .costPrice(new BigDecimal("25.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("進貨確認事件 - 更新庫存數量")
    void onPurchaseConfirmed_UpdatesStockQuantity() {
        // Given
        PurchaseConfirmedEvent.PurchaseItemInfo itemInfo = new PurchaseConfirmedEvent.PurchaseItemInfo(
                1L, "M001", "測試原物料", new BigDecimal("10.00"), new BigDecimal("100.00")
        );
        event = new PurchaseConfirmedEvent(
                1L, "PO-20251205-0001", new BigDecimal("1000.00"), List.of(itemInfo)
        );

        when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
        when(materialRepository.save(any(Material.class))).thenReturn(testMaterial);

        // When
        listener.onPurchaseConfirmed(event);

        // Then
        ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
        verify(materialRepository).save(captor.capture());

        Material savedMaterial = captor.getValue();
        // 原始 50 + 進貨 10 = 60
        assertThat(savedMaterial.getCurrentStockQuantity()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    @DisplayName("進貨確認事件 - 更新成本單價")
    void onPurchaseConfirmed_UpdatesCostPrice() {
        // Given
        PurchaseConfirmedEvent.PurchaseItemInfo itemInfo = new PurchaseConfirmedEvent.PurchaseItemInfo(
                1L, "M001", "測試原物料", new BigDecimal("10.00"), new BigDecimal("120.00")
        );
        event = new PurchaseConfirmedEvent(
                1L, "PO-20251205-0001", new BigDecimal("1200.00"), List.of(itemInfo)
        );

        when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
        when(materialRepository.save(any(Material.class))).thenReturn(testMaterial);

        // When
        listener.onPurchaseConfirmed(event);

        // Then
        ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
        verify(materialRepository).save(captor.capture());

        Material savedMaterial = captor.getValue();
        // 成本單價應更新為最新進貨單價
        assertThat(savedMaterial.getCostPrice()).isEqualByComparingTo(new BigDecimal("120.00"));
    }

    @Test
    @DisplayName("進貨確認事件 - 原物料不存在不拋出異常")
    void onPurchaseConfirmed_MaterialNotFound_DoesNotThrow() {
        // Given
        PurchaseConfirmedEvent.PurchaseItemInfo itemInfo = new PurchaseConfirmedEvent.PurchaseItemInfo(
                99L, "M999", "不存在的原物料", new BigDecimal("10.00"), new BigDecimal("100.00")
        );
        event = new PurchaseConfirmedEvent(
                1L, "PO-20251205-0001", new BigDecimal("1000.00"), List.of(itemInfo)
        );

        when(materialRepository.findById(99L)).thenReturn(Optional.empty());

        // When - 不應拋出異常
        listener.onPurchaseConfirmed(event);

        // Then
        verify(materialRepository).findById(99L);
        verify(materialRepository, never()).save(any());
    }

    @Test
    @DisplayName("進貨確認事件 - 多個明細項目")
    void onPurchaseConfirmed_MultipleItems() {
        // Given
        Material material2 = TestDataFactory.defaultMaterial()
                .id(2L)
                .code("M002")
                .name("第二個原物料")
                .currentStockQuantity(new BigDecimal("30.00"))
                .costPrice(new BigDecimal("50.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PurchaseConfirmedEvent.PurchaseItemInfo itemInfo1 = new PurchaseConfirmedEvent.PurchaseItemInfo(
                1L, "M001", "測試原物料", new BigDecimal("10.00"), new BigDecimal("100.00")
        );
        PurchaseConfirmedEvent.PurchaseItemInfo itemInfo2 = new PurchaseConfirmedEvent.PurchaseItemInfo(
                2L, "M002", "第二個原物料", new BigDecimal("20.00"), new BigDecimal("60.00")
        );
        event = new PurchaseConfirmedEvent(
                1L, "PO-20251205-0001", new BigDecimal("2200.00"), List.of(itemInfo1, itemInfo2)
        );

        when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
        when(materialRepository.findById(2L)).thenReturn(Optional.of(material2));
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        listener.onPurchaseConfirmed(event);

        // Then
        verify(materialRepository, times(2)).save(any(Material.class));
    }

    @Test
    @DisplayName("進貨確認事件 - 原始庫存為 null")
    void onPurchaseConfirmed_NullStock_TreatsAsZero() {
        // Given
        Material materialWithNullStock = TestDataFactory.defaultMaterial()
                .id(1L)
                .currentStockQuantity(null)  // null 庫存
                .costPrice(new BigDecimal("25.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PurchaseConfirmedEvent.PurchaseItemInfo itemInfo = new PurchaseConfirmedEvent.PurchaseItemInfo(
                1L, "M001", "測試原物料", new BigDecimal("10.00"), new BigDecimal("100.00")
        );
        event = new PurchaseConfirmedEvent(
                1L, "PO-20251205-0001", new BigDecimal("1000.00"), List.of(itemInfo)
        );

        when(materialRepository.findById(1L)).thenReturn(Optional.of(materialWithNullStock));
        when(materialRepository.save(any(Material.class))).thenReturn(materialWithNullStock);

        // When
        listener.onPurchaseConfirmed(event);

        // Then
        ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
        verify(materialRepository).save(captor.capture());

        Material savedMaterial = captor.getValue();
        // null 視為 0，0 + 10 = 10
        assertThat(savedMaterial.getCurrentStockQuantity()).isEqualByComparingTo(new BigDecimal("10.00"));
    }
}
