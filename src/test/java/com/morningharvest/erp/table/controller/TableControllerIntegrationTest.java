package com.morningharvest.erp.table.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.table.constant.TableStatus;
import com.morningharvest.erp.table.dto.CreateTableRequest;
import com.morningharvest.erp.table.dto.UpdateTableRequest;
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
@DisplayName("TableController 整合測試")
class TableControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DiningTableRepository diningTableRepository;

    private DiningTable testTable;

    @BeforeEach
    void setUp() {
        diningTableRepository.deleteAll();

        testTable = defaultTable()
                .tableNumber("T01")
                .capacity(4)
                .note("測試桌位")
                .build();
        testTable = diningTableRepository.save(testTable);
    }

    // ===== Create =====

    @Test
    @DisplayName("POST /api/tables/create - 建立桌位成功")
    void createTable_Success() throws Exception {
        CreateTableRequest request = new CreateTableRequest();
        request.setTableNumber("T02");
        request.setCapacity(6);
        request.setNote("新建桌位");

        mockMvc.perform(post("/api/tables/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tableNumber").value("T02"))
                .andExpect(jsonPath("$.data.capacity").value(6))
                .andExpect(jsonPath("$.data.status").value(TableStatus.AVAILABLE))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("POST /api/tables/create - 使用預設容納人數")
    void createTable_WithDefaultCapacity() throws Exception {
        CreateTableRequest request = new CreateTableRequest();
        request.setTableNumber("T03");
        // capacity 不設定，使用預設值

        mockMvc.perform(post("/api/tables/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.capacity").value(4)); // 預設值
    }

    @Test
    @DisplayName("POST /api/tables/create - 參數驗證失敗 (code=2001)")
    void createTable_ValidationError() throws Exception {
        // 缺少必填欄位 tableNumber
        String invalidRequest = "{ \"capacity\": 4 }";

        mockMvc.perform(post("/api/tables/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.tableNumber").exists());
    }

    @Test
    @DisplayName("POST /api/tables/create - 桌號長度超過限制 (code=2001)")
    void createTable_TableNumberTooLong() throws Exception {
        CreateTableRequest request = new CreateTableRequest();
        request.setTableNumber("A".repeat(25)); // 超過 20 字元
        request.setCapacity(4);

        mockMvc.perform(post("/api/tables/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/tables/create - 桌號重複 (code=2002)")
    void createTable_DuplicateTableNumber() throws Exception {
        CreateTableRequest request = new CreateTableRequest();
        request.setTableNumber("T01"); // 與 testTable 同號
        request.setCapacity(4);

        mockMvc.perform(post("/api/tables/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("桌號已存在: T01"));
    }

    // ===== Detail =====

    @Test
    @DisplayName("GET /api/tables/detail - 查詢桌位成功")
    void getTableDetail_Success() throws Exception {
        mockMvc.perform(get("/api/tables/detail")
                        .param("id", testTable.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testTable.getId()))
                .andExpect(jsonPath("$.data.tableNumber").value("T01"))
                .andExpect(jsonPath("$.data.capacity").value(4));
    }

    @Test
    @DisplayName("GET /api/tables/detail - 桌位不存在 (code=3001)")
    void getTableDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/tables/detail")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("桌位不存在: 99999"));
    }

    // ===== List =====

    @Test
    @DisplayName("GET /api/tables/list - 分頁查詢成功")
    void listTables_Success() throws Exception {
        mockMvc.perform(get("/api/tables/list")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/tables/list - 篩選狀態查詢 (空桌)")
    void listTables_FilterByAvailableStatus() throws Exception {
        // 新增一個佔用中的桌位
        DiningTable occupiedTable = occupiedTable()
                .tableNumber("T02")
                .build();
        diningTableRepository.save(occupiedTable);

        // 查詢空桌
        mockMvc.perform(get("/api/tables/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("status", TableStatus.AVAILABLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].tableNumber").value("T01"));
    }

    @Test
    @DisplayName("GET /api/tables/list - 篩選狀態查詢 (佔用中)")
    void listTables_FilterByOccupiedStatus() throws Exception {
        // 新增一個佔用中的桌位
        DiningTable occupiedTable = occupiedTable()
                .tableNumber("T02")
                .build();
        diningTableRepository.save(occupiedTable);

        // 查詢佔用中桌位
        mockMvc.perform(get("/api/tables/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("status", TableStatus.OCCUPIED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].tableNumber").value("T02"));
    }

    @Test
    @DisplayName("GET /api/tables/list - 排序功能")
    void listTables_WithSorting() throws Exception {
        // 新增更多桌位
        diningTableRepository.save(defaultTable().tableNumber("T03").build());
        diningTableRepository.save(defaultTable().tableNumber("T02").build());

        // 按桌號升序排序
        mockMvc.perform(get("/api/tables/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sortBy", "tableNumber")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.content[0].tableNumber").value("T01"))
                .andExpect(jsonPath("$.data.content[1].tableNumber").value("T02"))
                .andExpect(jsonPath("$.data.content[2].tableNumber").value("T03"));
    }

    // ===== Update =====

    @Test
    @DisplayName("POST /api/tables/update - 更新桌位成功")
    void updateTable_Success() throws Exception {
        UpdateTableRequest request = new UpdateTableRequest();
        request.setTableNumber("T01-updated");
        request.setCapacity(8);
        request.setNote("更新後的備註");

        mockMvc.perform(post("/api/tables/update")
                        .param("id", testTable.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tableNumber").value("T01-updated"))
                .andExpect(jsonPath("$.data.capacity").value(8));
    }

    @Test
    @DisplayName("POST /api/tables/update - 部分更新成功")
    void updateTable_PartialUpdate() throws Exception {
        UpdateTableRequest request = new UpdateTableRequest();
        request.setCapacity(10); // 只更新容納人數

        mockMvc.perform(post("/api/tables/update")
                        .param("id", testTable.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.tableNumber").value("T01")) // 維持原值
                .andExpect(jsonPath("$.data.capacity").value(10)); // 已更新
    }

    @Test
    @DisplayName("POST /api/tables/update - 桌位不存在 (code=3001)")
    void updateTable_NotFound() throws Exception {
        UpdateTableRequest request = new UpdateTableRequest();
        request.setCapacity(6);

        mockMvc.perform(post("/api/tables/update")
                        .param("id", "99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/tables/update - 桌號重複 (code=2002)")
    void updateTable_DuplicateTableNumber() throws Exception {
        // 新增另一個桌位
        DiningTable anotherTable = defaultTable()
                .tableNumber("T02")
                .build();
        diningTableRepository.save(anotherTable);

        // 嘗試將 testTable 的桌號更新為 T02
        UpdateTableRequest request = new UpdateTableRequest();
        request.setTableNumber("T02");

        mockMvc.perform(post("/api/tables/update")
                        .param("id", testTable.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("桌號已存在: T02"));
    }

    // ===== Delete =====

    @Test
    @DisplayName("POST /api/tables/delete - 刪除桌位成功")
    void deleteTable_Success() throws Exception {
        mockMvc.perform(post("/api/tables/delete")
                        .param("id", testTable.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true));

        // 驗證軟刪除
        DiningTable deleted = diningTableRepository.findById(testTable.getId()).orElse(null);
        assertThat(deleted).isNotNull();
        assertThat(deleted.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("POST /api/tables/delete - 桌位不存在 (code=3001)")
    void deleteTable_NotFound() throws Exception {
        mockMvc.perform(post("/api/tables/delete")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/tables/delete - 無法刪除佔用中桌位 (code=2002)")
    void deleteTable_OccupiedTable() throws Exception {
        // 將桌位設為佔用中
        testTable.setStatus(TableStatus.OCCUPIED);
        testTable.setCurrentOrderId(100L);
        diningTableRepository.save(testTable);

        mockMvc.perform(post("/api/tables/delete")
                        .param("id", testTable.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("無法刪除佔用中的桌位"));
    }
}
