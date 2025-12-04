package com.morningharvest.erp.pos.service;

import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.order.dto.OrderDTO;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.repository.OrderRepository;
import com.morningharvest.erp.pos.dto.EndDiningRequest;
import com.morningharvest.erp.pos.dto.SeatRequest;
import com.morningharvest.erp.pos.dto.TransferTableRequest;
import com.morningharvest.erp.table.constant.TableStatus;
import com.morningharvest.erp.table.dto.TableDTO;
import com.morningharvest.erp.table.dto.TableWithOrderDTO;
import com.morningharvest.erp.table.entity.DiningTable;
import com.morningharvest.erp.table.repository.DiningTableRepository;
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

import static com.morningharvest.erp.common.test.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PosTableService 單元測試")
class PosTableServiceTest {

    @Mock
    private DiningTableRepository diningTableRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PosTableService posTableService;

    private DiningTable availableTable;
    private DiningTable occupiedTable;
    private Order draftOrder;
    private Order paidOrder;

    @BeforeEach
    void setUp() {
        availableTable = defaultTable()
                .id(1L)
                .tableNumber("A1")
                .createdAt(LocalDateTime.now())
                .build();

        occupiedTable = occupiedTable()
                .id(2L)
                .tableNumber("A2")
                .currentOrderId(100L)
                .createdAt(LocalDateTime.now())
                .build();

        draftOrder = draftOrder()
                .id(100L)
                .build();

        paidOrder = Order.builder()
                .id(101L)
                .status("PAID")
                .orderType("DINE_IN")
                .totalAmount(new BigDecimal("500.00"))
                .build();
    }

    // ===== listTablesWithOrders =====

    @Test
    @DisplayName("查詢所有桌位含訂單資訊 - 成功")
    void listTablesWithOrders_Success() {
        // Given
        when(diningTableRepository.findByIsActiveTrueOrderByTableNumberAsc())
                .thenReturn(List.of(availableTable, occupiedTable));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(draftOrder));

        // When
        List<TableWithOrderDTO> result = posTableService.listTablesWithOrders();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCurrentOrder()).isNull(); // 空桌無訂單
        assertThat(result.get(1).getCurrentOrder()).isNotNull(); // 佔用桌有訂單
        assertThat(result.get(1).getCurrentOrder().getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("查詢所有桌位含訂單資訊 - 訂單不存在時回傳 null")
    void listTablesWithOrders_OrderNotFound_ReturnsNull() {
        // Given
        when(diningTableRepository.findByIsActiveTrueOrderByTableNumberAsc())
                .thenReturn(List.of(occupiedTable));
        when(orderRepository.findById(100L)).thenReturn(Optional.empty());

        // When
        List<TableWithOrderDTO> result = posTableService.listTablesWithOrders();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrentOrder()).isNull();
    }

    // ===== listAvailableTables =====

    @Test
    @DisplayName("查詢空桌列表 - 成功")
    void listAvailableTables_Success() {
        // Given
        when(diningTableRepository.findByStatusAndIsActiveTrueOrderByTableNumberAsc(TableStatus.AVAILABLE))
                .thenReturn(List.of(availableTable));

        // When
        List<TableDTO> result = posTableService.listAvailableTables();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(TableStatus.AVAILABLE);
    }

    // ===== seat =====

    @Test
    @DisplayName("入座 - 建立新訂單成功")
    void seat_WithNewOrder_Success() {
        // Given
        SeatRequest request = new SeatRequest();
        request.setTableId(1L);
        // orderId = null, 會建立新訂單

        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(availableTable));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(200L);
            return o;
        });
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TableWithOrderDTO result = posTableService.seat(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(result.getCurrentOrder()).isNotNull();
        assertThat(result.getCurrentOrder().getOrderType()).isEqualTo("DINE_IN");
        verify(orderRepository).save(any(Order.class));
        verify(diningTableRepository).save(argThat(t ->
                TableStatus.OCCUPIED.equals(t.getStatus()) && t.getCurrentOrderId() != null));
    }

    @Test
    @DisplayName("入座 - 綁定既有訂單成功")
    void seat_WithExistingOrder_Success() {
        // Given
        SeatRequest request = new SeatRequest();
        request.setTableId(1L);
        request.setOrderId(100L);

        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(availableTable));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(draftOrder));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TableWithOrderDTO result = posTableService.seat(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(result.getCurrentOrder().getId()).isEqualTo(100L);
        verify(orderRepository, never()).save(any(Order.class)); // 不建立新訂單
    }

    @Test
    @DisplayName("入座 - 桌位不存在拋出例外")
    void seat_TableNotFound_ThrowsException() {
        // Given
        SeatRequest request = new SeatRequest();
        request.setTableId(999L);

        when(diningTableRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> posTableService.seat(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("桌位不存在");
    }

    @Test
    @DisplayName("入座 - 桌位已停用拋出例外")
    void seat_TableInactive_ThrowsException() {
        // Given
        DiningTable inactiveTable = inactiveTable()
                .id(3L)
                .build();

        SeatRequest request = new SeatRequest();
        request.setTableId(3L);

        when(diningTableRepository.findById(3L)).thenReturn(Optional.of(inactiveTable));

        // When & Then
        assertThatThrownBy(() -> posTableService.seat(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("桌位已停用");
    }

    @Test
    @DisplayName("入座 - 桌位已被佔用拋出例外")
    void seat_TableOccupied_ThrowsException() {
        // Given
        SeatRequest request = new SeatRequest();
        request.setTableId(2L);

        when(diningTableRepository.findById(2L)).thenReturn(Optional.of(occupiedTable));

        // When & Then
        assertThatThrownBy(() -> posTableService.seat(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("桌位已被佔用");
    }

    @Test
    @DisplayName("入座 - 訂單不存在拋出例外")
    void seat_OrderNotFound_ThrowsException() {
        // Given
        SeatRequest request = new SeatRequest();
        request.setTableId(1L);
        request.setOrderId(999L);

        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(availableTable));
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> posTableService.seat(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("訂單不存在");
    }

    @Test
    @DisplayName("入座 - 只能綁定草稿訂單")
    void seat_OrderNotDraft_ThrowsException() {
        // Given
        SeatRequest request = new SeatRequest();
        request.setTableId(1L);
        request.setOrderId(101L);

        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(availableTable));
        when(orderRepository.findById(101L)).thenReturn(Optional.of(paidOrder));

        // When & Then
        assertThatThrownBy(() -> posTableService.seat(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只能綁定草稿狀態的訂單");
    }

    // ===== transfer =====

    @Test
    @DisplayName("換桌 - 成功")
    void transfer_Success() {
        // Given
        DiningTable targetTable = defaultTable()
                .id(3L)
                .tableNumber("A3")
                .build();

        TransferTableRequest request = new TransferTableRequest();
        request.setFromTableId(2L);
        request.setToTableId(3L);

        when(diningTableRepository.findById(2L)).thenReturn(Optional.of(occupiedTable));
        when(diningTableRepository.findById(3L)).thenReturn(Optional.of(targetTable));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(draftOrder));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TableWithOrderDTO result = posTableService.transfer(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(result.getCurrentOrder().getId()).isEqualTo(100L);

        // 驗證兩個桌位都被儲存
        verify(diningTableRepository, times(2)).save(any(DiningTable.class));
    }

    @Test
    @DisplayName("換桌 - 來源桌位不存在拋出例外")
    void transfer_FromTableNotFound_ThrowsException() {
        // Given
        TransferTableRequest request = new TransferTableRequest();
        request.setFromTableId(999L);
        request.setToTableId(3L);

        when(diningTableRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> posTableService.transfer(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("來源桌位不存在");
    }

    @Test
    @DisplayName("換桌 - 來源桌位非佔用狀態拋出例外")
    void transfer_FromTableNotOccupied_ThrowsException() {
        // Given
        TransferTableRequest request = new TransferTableRequest();
        request.setFromTableId(1L);
        request.setToTableId(3L);

        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(availableTable));

        // When & Then
        assertThatThrownBy(() -> posTableService.transfer(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("來源桌位非佔用狀態");
    }

    @Test
    @DisplayName("換桌 - 來源桌位沒有訂單拋出例外")
    void transfer_FromTableNoOrder_ThrowsException() {
        // Given
        DiningTable tableWithoutOrder = occupiedTable()
                .id(4L)
                .currentOrderId(null)
                .build();

        TransferTableRequest request = new TransferTableRequest();
        request.setFromTableId(4L);
        request.setToTableId(3L);

        when(diningTableRepository.findById(4L)).thenReturn(Optional.of(tableWithoutOrder));

        // When & Then
        assertThatThrownBy(() -> posTableService.transfer(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("來源桌位沒有訂單");
    }

    @Test
    @DisplayName("換桌 - 目標桌位不存在拋出例外")
    void transfer_ToTableNotFound_ThrowsException() {
        // Given
        TransferTableRequest request = new TransferTableRequest();
        request.setFromTableId(2L);
        request.setToTableId(999L);

        when(diningTableRepository.findById(2L)).thenReturn(Optional.of(occupiedTable));
        when(diningTableRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> posTableService.transfer(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("目標桌位不存在");
    }

    @Test
    @DisplayName("換桌 - 目標桌位已停用拋出例外")
    void transfer_ToTableInactive_ThrowsException() {
        // Given
        DiningTable inactiveTarget = inactiveTable()
                .id(5L)
                .build();

        TransferTableRequest request = new TransferTableRequest();
        request.setFromTableId(2L);
        request.setToTableId(5L);

        when(diningTableRepository.findById(2L)).thenReturn(Optional.of(occupiedTable));
        when(diningTableRepository.findById(5L)).thenReturn(Optional.of(inactiveTarget));

        // When & Then
        assertThatThrownBy(() -> posTableService.transfer(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("目標桌位已停用");
    }

    @Test
    @DisplayName("換桌 - 目標桌位已被佔用拋出例外")
    void transfer_ToTableOccupied_ThrowsException() {
        // Given
        DiningTable anotherOccupiedTable = occupiedTable()
                .id(6L)
                .tableNumber("A6")
                .currentOrderId(200L)
                .build();

        TransferTableRequest request = new TransferTableRequest();
        request.setFromTableId(2L);
        request.setToTableId(6L);

        when(diningTableRepository.findById(2L)).thenReturn(Optional.of(occupiedTable));
        when(diningTableRepository.findById(6L)).thenReturn(Optional.of(anotherOccupiedTable));

        // When & Then
        assertThatThrownBy(() -> posTableService.transfer(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("目標桌位已被佔用");
    }

    // ===== endDining =====

    @Test
    @DisplayName("結束用餐 - 成功 (訂單已付款)")
    void endDining_WithPaidOrder_Success() {
        // Given
        occupiedTable.setCurrentOrderId(101L);

        EndDiningRequest request = new EndDiningRequest();
        request.setTableId(2L);

        when(diningTableRepository.findById(2L)).thenReturn(Optional.of(occupiedTable));
        when(orderRepository.findById(101L)).thenReturn(Optional.of(paidOrder));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TableDTO result = posTableService.endDining(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(result.getCurrentOrderId()).isNull();
        verify(diningTableRepository).save(argThat(t ->
                TableStatus.AVAILABLE.equals(t.getStatus()) && t.getCurrentOrderId() == null));
    }

    @Test
    @DisplayName("結束用餐 - 成功 (訂單已完成)")
    void endDining_WithCompletedOrder_Success() {
        // Given
        Order completedOrder = completedOrder()
                .id(102L)
                .build();
        occupiedTable.setCurrentOrderId(102L);

        EndDiningRequest request = new EndDiningRequest();
        request.setTableId(2L);

        when(diningTableRepository.findById(2L)).thenReturn(Optional.of(occupiedTable));
        when(orderRepository.findById(102L)).thenReturn(Optional.of(completedOrder));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TableDTO result = posTableService.endDining(request);

        // Then
        assertThat(result.getStatus()).isEqualTo(TableStatus.AVAILABLE);
    }

    @Test
    @DisplayName("結束用餐 - 成功 (訂單已取消)")
    void endDining_WithCancelledOrder_Success() {
        // Given
        Order cancelledOrder = Order.builder()
                .id(103L)
                .status("CANCELLED")
                .build();
        occupiedTable.setCurrentOrderId(103L);

        EndDiningRequest request = new EndDiningRequest();
        request.setTableId(2L);

        when(diningTableRepository.findById(2L)).thenReturn(Optional.of(occupiedTable));
        when(orderRepository.findById(103L)).thenReturn(Optional.of(cancelledOrder));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TableDTO result = posTableService.endDining(request);

        // Then
        assertThat(result.getStatus()).isEqualTo(TableStatus.AVAILABLE);
    }

    @Test
    @DisplayName("結束用餐 - 桌位不存在拋出例外")
    void endDining_TableNotFound_ThrowsException() {
        // Given
        EndDiningRequest request = new EndDiningRequest();
        request.setTableId(999L);

        when(diningTableRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> posTableService.endDining(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("桌位不存在");
    }

    @Test
    @DisplayName("結束用餐 - 桌位非佔用狀態拋出例外")
    void endDining_TableNotOccupied_ThrowsException() {
        // Given
        EndDiningRequest request = new EndDiningRequest();
        request.setTableId(1L);

        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(availableTable));

        // When & Then
        assertThatThrownBy(() -> posTableService.endDining(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("桌位非佔用狀態");
    }

    @Test
    @DisplayName("結束用餐 - 訂單尚未結帳拋出例外")
    void endDining_OrderNotPaid_ThrowsException() {
        // Given
        EndDiningRequest request = new EndDiningRequest();
        request.setTableId(2L);

        when(diningTableRepository.findById(2L)).thenReturn(Optional.of(occupiedTable));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(draftOrder));

        // When & Then
        assertThatThrownBy(() -> posTableService.endDining(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("訂單尚未結帳或完成");
    }
}
