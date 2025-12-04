package com.morningharvest.erp.table.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.table.constant.TableStatus;
import com.morningharvest.erp.table.dto.CreateTableRequest;
import com.morningharvest.erp.table.dto.TableDTO;
import com.morningharvest.erp.table.dto.UpdateTableRequest;
import com.morningharvest.erp.table.entity.DiningTable;
import com.morningharvest.erp.table.repository.DiningTableRepository;
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

import static com.morningharvest.erp.common.test.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TableService 單元測試")
class TableServiceTest {

    @Mock
    private DiningTableRepository diningTableRepository;

    @InjectMocks
    private TableService tableService;

    private DiningTable testTable;
    private CreateTableRequest createRequest;
    private UpdateTableRequest updateRequest;

    @BeforeEach
    void setUp() {
        testTable = defaultTable()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = new CreateTableRequest();
        createRequest.setTableNumber("B1");
        createRequest.setCapacity(6);
        createRequest.setNote("靠窗座位");

        updateRequest = new UpdateTableRequest();
        updateRequest.setTableNumber("B2");
        updateRequest.setCapacity(8);
        updateRequest.setNote("更新備註");
    }

    @Test
    @DisplayName("建立桌位 - 成功")
    void createTable_Success() {
        // Given
        when(diningTableRepository.existsByTableNumberAndIsActiveTrue(anyString())).thenReturn(false);
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> {
            DiningTable t = invocation.getArgument(0);
            t.setId(1L);
            t.setCreatedAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
            return t;
        });

        // When
        TableDTO result = tableService.createTable(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTableNumber()).isEqualTo(createRequest.getTableNumber());
        assertThat(result.getCapacity()).isEqualTo(createRequest.getCapacity());
        assertThat(result.getStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(result.getIsActive()).isTrue();
        verify(diningTableRepository).existsByTableNumberAndIsActiveTrue(createRequest.getTableNumber());
        verify(diningTableRepository).save(any(DiningTable.class));
    }

    @Test
    @DisplayName("建立桌位 - 使用預設容納人數")
    void createTable_WithDefaultCapacity() {
        // Given
        createRequest.setCapacity(null);
        when(diningTableRepository.existsByTableNumberAndIsActiveTrue(anyString())).thenReturn(false);
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> {
            DiningTable t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        // When
        TableDTO result = tableService.createTable(createRequest);

        // Then
        assertThat(result.getCapacity()).isEqualTo(4); // 預設值
    }

    @Test
    @DisplayName("建立桌位 - 桌號重複拋出例外")
    void createTable_DuplicateTableNumber_ThrowsException() {
        // Given
        when(diningTableRepository.existsByTableNumberAndIsActiveTrue(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> tableService.createTable(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("桌號已存在");

        verify(diningTableRepository).existsByTableNumberAndIsActiveTrue(createRequest.getTableNumber());
        verify(diningTableRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新桌位 - 成功")
    void updateTable_Success() {
        // Given
        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(testTable));
        when(diningTableRepository.existsByTableNumberAndIdNotAndIsActiveTrue(anyString(), anyLong())).thenReturn(false);
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> {
            DiningTable t = invocation.getArgument(0);
            t.setUpdatedAt(LocalDateTime.now());
            return t;
        });

        // When
        TableDTO result = tableService.updateTable(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTableNumber()).isEqualTo(updateRequest.getTableNumber());
        assertThat(result.getCapacity()).isEqualTo(updateRequest.getCapacity());
        verify(diningTableRepository).findById(1L);
        verify(diningTableRepository).save(any(DiningTable.class));
    }

    @Test
    @DisplayName("更新桌位 - 部分更新")
    void updateTable_PartialUpdate() {
        // Given
        UpdateTableRequest partialRequest = new UpdateTableRequest();
        partialRequest.setCapacity(10); // 只更新容納人數

        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(testTable));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TableDTO result = tableService.updateTable(1L, partialRequest);

        // Then
        assertThat(result.getTableNumber()).isEqualTo(testTable.getTableNumber()); // 維持原值
        assertThat(result.getCapacity()).isEqualTo(10); // 已更新
    }

    @Test
    @DisplayName("更新桌位 - 桌位不存在拋出例外")
    void updateTable_NotFound_ThrowsException() {
        // Given
        when(diningTableRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tableService.updateTable(999L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("桌位不存在");

        verify(diningTableRepository).findById(999L);
        verify(diningTableRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新桌位 - 桌號與其他重複拋出例外")
    void updateTable_DuplicateTableNumber_ThrowsException() {
        // Given
        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(testTable));
        when(diningTableRepository.existsByTableNumberAndIdNotAndIsActiveTrue(anyString(), anyLong())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> tableService.updateTable(1L, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("桌號已存在");

        verify(diningTableRepository, never()).save(any());
    }

    @Test
    @DisplayName("刪除桌位 - 成功 (軟刪除)")
    void deleteTable_Success() {
        // Given
        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(testTable));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        tableService.deleteTable(1L);

        // Then
        verify(diningTableRepository).findById(1L);
        verify(diningTableRepository).save(argThat(table -> !table.getIsActive()));
    }

    @Test
    @DisplayName("刪除桌位 - 桌位不存在拋出例外")
    void deleteTable_NotFound_ThrowsException() {
        // Given
        when(diningTableRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tableService.deleteTable(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("桌位不存在");

        verify(diningTableRepository, never()).save(any());
    }

    @Test
    @DisplayName("刪除桌位 - 無法刪除佔用中桌位")
    void deleteTable_Occupied_ThrowsException() {
        // Given
        DiningTable occupiedTable = occupiedTable()
                .id(2L)
                .build();
        when(diningTableRepository.findById(2L)).thenReturn(Optional.of(occupiedTable));

        // When & Then
        assertThatThrownBy(() -> tableService.deleteTable(2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("無法刪除佔用中的桌位");

        verify(diningTableRepository, never()).save(any());
    }

    @Test
    @DisplayName("查詢桌位 - 成功")
    void getTableById_Success() {
        // Given
        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(testTable));

        // When
        TableDTO result = tableService.getTableById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testTable.getId());
        assertThat(result.getTableNumber()).isEqualTo(testTable.getTableNumber());
    }

    @Test
    @DisplayName("查詢桌位 - 桌位不存在拋出例外")
    void getTableById_NotFound_ThrowsException() {
        // Given
        when(diningTableRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tableService.getTableById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("桌位不存在");
    }

    @Test
    @DisplayName("分頁查詢桌位列表")
    void listTables_WithPagination() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<DiningTable> tablePage = new PageImpl<>(List.of(testTable));
        when(diningTableRepository.findByIsActiveTrue(any(Pageable.class))).thenReturn(tablePage);

        // When
        PageResponse<TableDTO> result = tableService.listTables(pageableRequest, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("分頁查詢桌位 - 篩選狀態")
    void listTables_FilterByStatus() {
        // Given
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(1)
                .size(10)
                .build();

        Page<DiningTable> tablePage = new PageImpl<>(List.of(testTable));
        when(diningTableRepository.findByStatusAndIsActiveTrue(eq(TableStatus.AVAILABLE), any(Pageable.class)))
                .thenReturn(tablePage);

        // When
        PageResponse<TableDTO> result = tableService.listTables(pageableRequest, TableStatus.AVAILABLE);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(diningTableRepository).findByStatusAndIsActiveTrue(eq(TableStatus.AVAILABLE), any(Pageable.class));
    }

    @Test
    @DisplayName("查詢所有啟用桌位")
    void listAllActiveTables_Success() {
        // Given
        DiningTable table2 = defaultTable()
                .id(2L)
                .tableNumber("A2")
                .build();
        when(diningTableRepository.findByIsActiveTrueOrderByTableNumberAsc())
                .thenReturn(List.of(testTable, table2));

        // When
        List<TableDTO> result = tableService.listAllActiveTables();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTableNumber()).isEqualTo("A1");
        assertThat(result.get(1).getTableNumber()).isEqualTo("A2");
    }

    @Test
    @DisplayName("查詢空桌")
    void listAvailableTables_Success() {
        // Given
        when(diningTableRepository.findByStatusAndIsActiveTrueOrderByTableNumberAsc(TableStatus.AVAILABLE))
                .thenReturn(List.of(testTable));

        // When
        List<TableDTO> result = tableService.listAvailableTables();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(TableStatus.AVAILABLE);
    }
}
