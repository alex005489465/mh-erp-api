package com.morningharvest.erp.inventorycheck.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.inventorycheck.constant.InventoryCheckStatus;
import com.morningharvest.erp.inventorycheck.dto.*;
import com.morningharvest.erp.inventorycheck.entity.InventoryCheck;
import com.morningharvest.erp.inventorycheck.entity.InventoryCheckItem;
import com.morningharvest.erp.inventorycheck.repository.InventoryCheckItemRepository;
import com.morningharvest.erp.inventorycheck.repository.InventoryCheckRepository;
import com.morningharvest.erp.material.entity.Material;
import com.morningharvest.erp.material.repository.MaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("InventoryCheckController 整合測試")
class InventoryCheckControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryCheckRepository inventoryCheckRepository;

    @Autowired
    private InventoryCheckItemRepository inventoryCheckItemRepository;

    @Autowired
    private MaterialRepository materialRepository;

    private Material testMaterial;
    private InventoryCheck testInventoryCheck;
    private InventoryCheckItem testInventoryCheckItem;

    @BeforeEach
    void setUp() {
        // 清理資料
        inventoryCheckItemRepository.deleteAll();
        inventoryCheckRepository.deleteAll();

        // 建立測試原物料
        testMaterial = Material.builder()
                .code("M001")
                .name("測試原物料")
                .unit("PIECE")
                .category("OTHER")
                .currentStockQuantity(new BigDecimal("50.00"))
                .costPrice(new BigDecimal("25.00"))
                .isActive(true)
                .build();
        testMaterial = materialRepository.save(testMaterial);

        // 建立測試盤點單
        testInventoryCheck = InventoryCheck.builder()
                .checkNumber("IC-20251205-0001")
                .status(InventoryCheckStatus.PLANNED)
                .checkDate(LocalDate.now())
                .totalItems(1)
                .totalDifferenceAmount(BigDecimal.ZERO)
                .build();
        testInventoryCheck = inventoryCheckRepository.save(testInventoryCheck);

        // 建立測試盤點明細
        testInventoryCheckItem = InventoryCheckItem.builder()
                .inventoryCheckId(testInventoryCheck.getId())
                .materialId(testMaterial.getId())
                .materialCode(testMaterial.getCode())
                .materialName(testMaterial.getName())
                .materialUnit(testMaterial.getUnit())
                .systemQuantity(testMaterial.getCurrentStockQuantity())
                .unitCost(testMaterial.getCostPrice())
                .isChecked(false)
                .build();
        testInventoryCheckItem = inventoryCheckItemRepository.save(testInventoryCheckItem);
    }

    @Nested
    @DisplayName("POST /api/inventory-checks/create 測試")
    class CreateInventoryCheckTests {

        @Test
        @DisplayName("建立盤點計畫成功")
        void createInventoryCheck_Success() throws Exception {
            CreateInventoryCheckRequest request = CreateInventoryCheckRequest.builder()
                    .checkDate(LocalDate.now())
                    .note("測試盤點")
                    .build();

            mockMvc.perform(post("/api/inventory-checks/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.checkNumber").isNotEmpty())
                    .andExpect(jsonPath("$.data.status").value(InventoryCheckStatus.PLANNED))
                    .andExpect(jsonPath("$.data.items").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/inventory-checks/start 測試")
    class StartInventoryCheckTests {

        @Test
        @DisplayName("開始盤點成功")
        void startInventoryCheck_Success() throws Exception {
            StartInventoryCheckRequest request = StartInventoryCheckRequest.builder()
                    .id(testInventoryCheck.getId())
                    .startedBy("admin")
                    .build();

            mockMvc.perform(post("/api/inventory-checks/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value(InventoryCheckStatus.IN_PROGRESS))
                    .andExpect(jsonPath("$.data.startedBy").value("admin"));
        }

        @Test
        @DisplayName("盤點單不存在返回 3001")
        void startInventoryCheck_NotFound() throws Exception {
            StartInventoryCheckRequest request = StartInventoryCheckRequest.builder()
                    .id(99999L)
                    .build();

            mockMvc.perform(post("/api/inventory-checks/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(3001))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("盤點單不存在")));
        }

        @Test
        @DisplayName("狀態非 PLANNED 返回 2003")
        void startInventoryCheck_InvalidStatus() throws Exception {
            // 將盤點單設為 IN_PROGRESS
            testInventoryCheck.setStatus(InventoryCheckStatus.IN_PROGRESS);
            testInventoryCheck.setStartedAt(LocalDateTime.now());
            inventoryCheckRepository.save(testInventoryCheck);

            StartInventoryCheckRequest request = StartInventoryCheckRequest.builder()
                    .id(testInventoryCheck.getId())
                    .build();

            mockMvc.perform(post("/api/inventory-checks/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(2003))
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/inventory-checks/update-item 測試")
    class UpdateInventoryCheckItemTests {

        @BeforeEach
        void setUpInProgress() {
            // 將盤點單設為 IN_PROGRESS
            testInventoryCheck.setStatus(InventoryCheckStatus.IN_PROGRESS);
            testInventoryCheck.setStartedAt(LocalDateTime.now());
            testInventoryCheck.setStartedBy("admin");
            inventoryCheckRepository.save(testInventoryCheck);
        }

        @Test
        @DisplayName("更新盤點數量成功")
        void updateInventoryCheckItem_Success() throws Exception {
            UpdateInventoryCheckItemRequest request = UpdateInventoryCheckItemRequest.builder()
                    .itemId(testInventoryCheckItem.getId())
                    .actualQuantity(new BigDecimal("48.00"))
                    .note("實際盤點")
                    .build();

            mockMvc.perform(post("/api/inventory-checks/update-item")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.actualQuantity").value(48.00))
                    .andExpect(jsonPath("$.data.isChecked").value(true))
                    .andExpect(jsonPath("$.data.differenceQuantity").value(-2.00));
        }

        @Test
        @DisplayName("參數驗證失敗返回 2001")
        void updateInventoryCheckItem_ValidationError() throws Exception {
            // actualQuantity 為負數
            String invalidRequest = """
                    {
                        "itemId": %d,
                        "actualQuantity": -10.00
                    }
                    """.formatted(testInventoryCheckItem.getId());

            mockMvc.perform(post("/api/inventory-checks/update-item")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(2001))
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("明細不存在返回 3001")
        void updateInventoryCheckItem_ItemNotFound() throws Exception {
            UpdateInventoryCheckItemRequest request = UpdateInventoryCheckItemRequest.builder()
                    .itemId(99999L)
                    .actualQuantity(new BigDecimal("48.00"))
                    .build();

            mockMvc.perform(post("/api/inventory-checks/update-item")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(3001))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("盤點明細不存在")));
        }
    }

    @Nested
    @DisplayName("POST /api/inventory-checks/confirm 測試")
    class ConfirmInventoryCheckTests {

        @BeforeEach
        void setUpInProgressWithCheckedItems() {
            // 將盤點單設為 IN_PROGRESS
            testInventoryCheck.setStatus(InventoryCheckStatus.IN_PROGRESS);
            testInventoryCheck.setStartedAt(LocalDateTime.now());
            testInventoryCheck.setStartedBy("admin");
            inventoryCheckRepository.save(testInventoryCheck);

            // 將明細設為已盤點
            testInventoryCheckItem.setActualQuantity(new BigDecimal("48.00"));
            testInventoryCheckItem.setDifferenceQuantity(new BigDecimal("-2.00"));
            testInventoryCheckItem.setDifferenceAmount(new BigDecimal("-50.00"));
            testInventoryCheckItem.setIsChecked(true);
            inventoryCheckItemRepository.save(testInventoryCheckItem);
        }

        @Test
        @DisplayName("確認盤點成功")
        void confirmInventoryCheck_Success() throws Exception {
            ConfirmInventoryCheckRequest request = ConfirmInventoryCheckRequest.builder()
                    .id(testInventoryCheck.getId())
                    .confirmedBy("admin")
                    .build();

            mockMvc.perform(post("/api/inventory-checks/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value(InventoryCheckStatus.CONFIRMED))
                    .andExpect(jsonPath("$.data.confirmedBy").value("admin"));
        }

        @Test
        @DisplayName("有未盤點項目返回 2003")
        void confirmInventoryCheck_UncheckedItems() throws Exception {
            // 將明細設為未盤點
            testInventoryCheckItem.setIsChecked(false);
            testInventoryCheckItem.setActualQuantity(null);
            inventoryCheckItemRepository.save(testInventoryCheckItem);

            ConfirmInventoryCheckRequest request = ConfirmInventoryCheckRequest.builder()
                    .id(testInventoryCheck.getId())
                    .build();

            mockMvc.perform(post("/api/inventory-checks/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(2003))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("未盤點")));
        }
    }

    @Nested
    @DisplayName("POST /api/inventory-checks/delete 測試")
    class DeleteInventoryCheckTests {

        @Test
        @DisplayName("刪除盤點單成功")
        void deleteInventoryCheck_Success() throws Exception {
            mockMvc.perform(post("/api/inventory-checks/delete")
                            .param("id", testInventoryCheck.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("狀態非 PLANNED 返回 2003")
        void deleteInventoryCheck_InvalidStatus() throws Exception {
            // 將盤點單設為 IN_PROGRESS
            testInventoryCheck.setStatus(InventoryCheckStatus.IN_PROGRESS);
            inventoryCheckRepository.save(testInventoryCheck);

            mockMvc.perform(post("/api/inventory-checks/delete")
                            .param("id", testInventoryCheck.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(2003))
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/inventory-checks/detail 測試")
    class GetInventoryCheckDetailTests {

        @Test
        @DisplayName("查詢盤點單詳情成功")
        void getInventoryCheckDetail_Success() throws Exception {
            mockMvc.perform(get("/api/inventory-checks/detail")
                            .param("id", testInventoryCheck.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testInventoryCheck.getId()))
                    .andExpect(jsonPath("$.data.checkNumber").value(testInventoryCheck.getCheckNumber()))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items", hasSize(1)));
        }

        @Test
        @DisplayName("盤點單不存在返回 3001")
        void getInventoryCheckDetail_NotFound() throws Exception {
            mockMvc.perform(get("/api/inventory-checks/detail")
                            .param("id", "99999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(3001))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("盤點單不存在")));
        }
    }

    @Nested
    @DisplayName("GET /api/inventory-checks/list 測試")
    class ListInventoryChecksTests {

        @Test
        @DisplayName("分頁查詢成功")
        void listInventoryChecks_Success() throws Exception {
            mockMvc.perform(get("/api/inventory-checks/list")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)));
        }

        @Test
        @DisplayName("篩選查詢成功")
        void listInventoryChecks_WithFilters() throws Exception {
            mockMvc.perform(get("/api/inventory-checks/list")
                            .param("page", "1")
                            .param("size", "10")
                            .param("status", InventoryCheckStatus.PLANNED)
                            .param("keyword", "IC-"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }
    }
}
