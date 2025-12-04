package com.morningharvest.erp.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.repository.OrderRepository;
import com.morningharvest.erp.pos.dto.EndDiningRequest;
import com.morningharvest.erp.pos.dto.SeatRequest;
import com.morningharvest.erp.pos.dto.TransferTableRequest;
import com.morningharvest.erp.table.constant.TableStatus;
import com.morningharvest.erp.table.entity.DiningTable;
import com.morningharvest.erp.table.repository.DiningTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.morningharvest.erp.common.test.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("PosTableController 整合測試")
class PosTableControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DiningTableRepository diningTableRepository;

    @Autowired
    private OrderRepository orderRepository;

    private DiningTable availableTable;
    private DiningTable occupiedTable;
    private Order draftOrder;
    private Order paidOrder;

    @BeforeEach
    void setUp() {
        diningTableRepository.deleteAll();

        // 建立空桌
        availableTable = defaultTable()
                .tableNumber("P01")
                .build();
        availableTable = diningTableRepository.save(availableTable);

        // 建立草稿訂單
        draftOrder = draftOrder().build();
        draftOrder = orderRepository.save(draftOrder);

        // 建立佔用中的桌位 (綁定訂單)
        occupiedTable = occupiedTable()
                .tableNumber("P02")
                .currentOrderId(draftOrder.getId())
                .build();
        occupiedTable = diningTableRepository.save(occupiedTable);

        // 建立已付款訂單
        paidOrder = Order.builder()
                .status("PAID")
                .orderType("DINE_IN")
                .totalAmount(new BigDecimal("500.00"))
                .build();
        paidOrder = orderRepository.save(paidOrder);
    }

    // ===== List Tables =====

    @Test
    @DisplayName("GET /api/pos/tables/list - 查詢所有桌位成功")
    void listTables_Success() throws Exception {
        mockMvc.perform(get("/api/pos/tables/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/pos/tables/list - 查詢桌位含訂單資訊")
    void listTables_WithOrderInfo() throws Exception {
        mockMvc.perform(get("/api/pos/tables/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                // 使用陣列索引方式驗證，避免 JSONPath filter 的複雜行為
                // P01 (空桌) 排序在前，currentOrder 為 null
                .andExpect(jsonPath("$.data[0].tableNumber").value("P01"))
                .andExpect(jsonPath("$.data[0].currentOrder").doesNotExist())
                // P02 (佔用桌) 有訂單資料
                .andExpect(jsonPath("$.data[1].tableNumber").value("P02"))
                .andExpect(jsonPath("$.data[1].currentOrder.id").value(draftOrder.getId().intValue()));
    }

    // ===== Available Tables =====

    @Test
    @DisplayName("GET /api/pos/tables/available - 查詢空桌成功")
    void listAvailableTables_Success() throws Exception {
        mockMvc.perform(get("/api/pos/tables/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].tableNumber").value("P01"))
                .andExpect(jsonPath("$.data[0].status").value(TableStatus.AVAILABLE));
    }

    // ===== Seat =====

    @Test
    @DisplayName("POST /api/pos/tables/seat - 入座建立新訂單成功")
    void seat_WithNewOrder_Success() throws Exception {
        SeatRequest request = new SeatRequest();
        request.setTableId(availableTable.getId());
        // orderId = null, 會建立新訂單

        mockMvc.perform(post("/api/pos/tables/seat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value(TableStatus.OCCUPIED))
                .andExpect(jsonPath("$.data.currentOrder").exists())
                .andExpect(jsonPath("$.data.currentOrder.orderType").value("DINE_IN"))
                .andExpect(jsonPath("$.data.currentOrder.status").value("DRAFT"));

        // 驗證資料庫狀態
        DiningTable updated = diningTableRepository.findById(availableTable.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(updated.getCurrentOrderId()).isNotNull();
    }

    @Test
    @DisplayName("POST /api/pos/tables/seat - 入座綁定既有訂單成功")
    void seat_WithExistingOrder_Success() throws Exception {
        // 建立另一個草稿訂單
        Order anotherDraft = draftOrder().build();
        anotherDraft = orderRepository.save(anotherDraft);

        SeatRequest request = new SeatRequest();
        request.setTableId(availableTable.getId());
        request.setOrderId(anotherDraft.getId());

        mockMvc.perform(post("/api/pos/tables/seat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value(TableStatus.OCCUPIED))
                .andExpect(jsonPath("$.data.currentOrder.id").value(anotherDraft.getId()));
    }

    @Test
    @DisplayName("POST /api/pos/tables/seat - 參數驗證失敗 (code=2001)")
    void seat_ValidationError() throws Exception {
        // 缺少必填欄位 tableId
        String invalidRequest = "{ }";

        mockMvc.perform(post("/api/pos/tables/seat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.tableId").exists());
    }

    @Test
    @DisplayName("POST /api/pos/tables/seat - 桌位不存在 (code=3001)")
    void seat_TableNotFound() throws Exception {
        SeatRequest request = new SeatRequest();
        request.setTableId(99999L);

        mockMvc.perform(post("/api/pos/tables/seat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("桌位不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/pos/tables/seat - 桌位已被佔用 (code=2002)")
    void seat_TableOccupied() throws Exception {
        SeatRequest request = new SeatRequest();
        request.setTableId(occupiedTable.getId());

        mockMvc.perform(post("/api/pos/tables/seat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("桌位已被佔用: P02"));
    }

    @Test
    @DisplayName("POST /api/pos/tables/seat - 訂單不存在 (code=3001)")
    void seat_OrderNotFound() throws Exception {
        SeatRequest request = new SeatRequest();
        request.setTableId(availableTable.getId());
        request.setOrderId(99999L);

        mockMvc.perform(post("/api/pos/tables/seat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("訂單不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/pos/tables/seat - 只能綁定草稿訂單 (code=2002)")
    void seat_OrderNotDraft() throws Exception {
        SeatRequest request = new SeatRequest();
        request.setTableId(availableTable.getId());
        request.setOrderId(paidOrder.getId());

        mockMvc.perform(post("/api/pos/tables/seat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("只能綁定草稿狀態的訂單"));
    }

    // ===== Transfer =====

    @Test
    @DisplayName("POST /api/pos/tables/transfer - 換桌成功")
    void transfer_Success() throws Exception {
        // 建立另一個空桌作為目標
        DiningTable targetTable = defaultTable()
                .tableNumber("P03")
                .build();
        targetTable = diningTableRepository.save(targetTable);

        TransferTableRequest request = new TransferTableRequest();
        request.setFromTableId(occupiedTable.getId());
        request.setToTableId(targetTable.getId());

        mockMvc.perform(post("/api/pos/tables/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tableNumber").value("P03"))
                .andExpect(jsonPath("$.data.status").value(TableStatus.OCCUPIED))
                .andExpect(jsonPath("$.data.currentOrder.id").value(draftOrder.getId()));

        // 驗證來源桌位已清空
        DiningTable fromTable = diningTableRepository.findById(occupiedTable.getId()).orElse(null);
        assertThat(fromTable).isNotNull();
        assertThat(fromTable.getStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(fromTable.getCurrentOrderId()).isNull();

        // 驗證目標桌位已佔用
        DiningTable toTable = diningTableRepository.findById(targetTable.getId()).orElse(null);
        assertThat(toTable).isNotNull();
        assertThat(toTable.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(toTable.getCurrentOrderId()).isEqualTo(draftOrder.getId());
    }

    @Test
    @DisplayName("POST /api/pos/tables/transfer - 參數驗證失敗 (code=2001)")
    void transfer_ValidationError() throws Exception {
        // 缺少必填欄位
        String invalidRequest = "{ \"fromTableId\": 1 }";

        mockMvc.perform(post("/api/pos/tables/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.toTableId").exists());
    }

    @Test
    @DisplayName("POST /api/pos/tables/transfer - 來源桌位非佔用狀態 (code=2002)")
    void transfer_FromTableNotOccupied() throws Exception {
        TransferTableRequest request = new TransferTableRequest();
        request.setFromTableId(availableTable.getId());
        request.setToTableId(occupiedTable.getId());

        mockMvc.perform(post("/api/pos/tables/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("來源桌位非佔用狀態: P01"));
    }

    @Test
    @DisplayName("POST /api/pos/tables/transfer - 目標桌位已被佔用 (code=2002)")
    void transfer_ToTableOccupied() throws Exception {
        // 建立另一個佔用中的桌位
        DiningTable anotherOccupied = occupiedTable()
                .tableNumber("P04")
                .currentOrderId(paidOrder.getId())
                .build();
        anotherOccupied = diningTableRepository.save(anotherOccupied);

        TransferTableRequest request = new TransferTableRequest();
        request.setFromTableId(occupiedTable.getId());
        request.setToTableId(anotherOccupied.getId());

        mockMvc.perform(post("/api/pos/tables/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("目標桌位已被佔用: P04"));
    }

    // ===== End Dining =====

    @Test
    @DisplayName("POST /api/pos/tables/end - 結束用餐成功")
    void endDining_Success() throws Exception {
        // 將佔用桌位的訂單改為已付款
        occupiedTable.setCurrentOrderId(paidOrder.getId());
        diningTableRepository.save(occupiedTable);

        EndDiningRequest request = new EndDiningRequest();
        request.setTableId(occupiedTable.getId());

        mockMvc.perform(post("/api/pos/tables/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value(TableStatus.AVAILABLE))
                .andExpect(jsonPath("$.data.currentOrderId").isEmpty());

        // 驗證資料庫狀態
        DiningTable updated = diningTableRepository.findById(occupiedTable.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(updated.getCurrentOrderId()).isNull();
    }

    @Test
    @DisplayName("POST /api/pos/tables/end - 參數驗證失敗 (code=2001)")
    void endDining_ValidationError() throws Exception {
        // 缺少必填欄位
        String invalidRequest = "{ }";

        mockMvc.perform(post("/api/pos/tables/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.tableId").exists());
    }

    @Test
    @DisplayName("POST /api/pos/tables/end - 桌位不存在 (code=3001)")
    void endDining_TableNotFound() throws Exception {
        EndDiningRequest request = new EndDiningRequest();
        request.setTableId(99999L);

        mockMvc.perform(post("/api/pos/tables/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("桌位不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/pos/tables/end - 桌位非佔用狀態 (code=2002)")
    void endDining_TableNotOccupied() throws Exception {
        EndDiningRequest request = new EndDiningRequest();
        request.setTableId(availableTable.getId());

        mockMvc.perform(post("/api/pos/tables/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("桌位非佔用狀態: P01"));
    }

    @Test
    @DisplayName("POST /api/pos/tables/end - 訂單尚未結帳 (code=2002)")
    void endDining_OrderNotPaid() throws Exception {
        EndDiningRequest request = new EndDiningRequest();
        request.setTableId(occupiedTable.getId());

        mockMvc.perform(post("/api/pos/tables/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("訂單尚未結帳或完成，無法結束用餐"));
    }
}
