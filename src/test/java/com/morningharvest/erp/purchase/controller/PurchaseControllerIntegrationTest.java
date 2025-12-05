package com.morningharvest.erp.purchase.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.common.test.TestDataFactory;
import com.morningharvest.erp.material.entity.Material;
import com.morningharvest.erp.material.repository.MaterialRepository;
import com.morningharvest.erp.purchase.dto.ConfirmPurchaseRequest;
import com.morningharvest.erp.purchase.dto.CreatePurchaseItemRequest;
import com.morningharvest.erp.purchase.dto.CreatePurchaseRequest;
import com.morningharvest.erp.purchase.dto.UpdatePurchaseRequest;
import com.morningharvest.erp.purchase.entity.Purchase;
import com.morningharvest.erp.purchase.entity.PurchaseItem;
import com.morningharvest.erp.purchase.repository.PurchaseItemRepository;
import com.morningharvest.erp.purchase.repository.PurchaseRepository;
import com.morningharvest.erp.supplier.entity.Supplier;
import com.morningharvest.erp.supplier.repository.SupplierRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("PurchaseController 整合測試")
class PurchaseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private PurchaseItemRepository purchaseItemRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private MaterialRepository materialRepository;

    private Supplier testSupplier;
    private Material testMaterial;
    private Purchase testPurchase;
    private Purchase confirmedPurchase;
    private PurchaseItem testPurchaseItem;

    @BeforeEach
    void setUp() {
        // 清除測試資料
        purchaseItemRepository.deleteAll();
        purchaseRepository.deleteAll();
        supplierRepository.deleteAll();
        materialRepository.deleteAll();

        // 建立供應商
        testSupplier = supplierRepository.save(
                TestDataFactory.defaultSupplier().build()
        );

        // 建立原物料
        testMaterial = materialRepository.save(
                TestDataFactory.defaultMaterial().build()
        );

        // 建立草稿進貨單
        testPurchase = purchaseRepository.save(
                TestDataFactory.defaultPurchase()
                        .supplierId(testSupplier.getId())
                        .supplierName(testSupplier.getName())
                        .build()
        );

        // 建立進貨明細
        testPurchaseItem = purchaseItemRepository.save(
                TestDataFactory.defaultPurchaseItem()
                        .purchaseId(testPurchase.getId())
                        .materialId(testMaterial.getId())
                        .materialCode(testMaterial.getCode())
                        .materialName(testMaterial.getName())
                        .materialUnit(testMaterial.getUnit())
                        .build()
        );

        // 建立已確認進貨單
        confirmedPurchase = purchaseRepository.save(
                TestDataFactory.confirmedPurchase()
                        .purchaseNumber("PO-20251205-0002")
                        .supplierId(testSupplier.getId())
                        .supplierName(testSupplier.getName())
                        .build()
        );
    }

    // ===== GET /api/purchases/detail 測試 =====

    @Nested
    @DisplayName("GET /api/purchases/detail 測試")
    class GetDetailTests {

        @Test
        @DisplayName("查詢進貨單詳情 - 成功")
        void getPurchaseDetail_Success() throws Exception {
            mockMvc.perform(get("/api/purchases/detail")
                            .param("id", testPurchase.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testPurchase.getId()))
                    .andExpect(jsonPath("$.data.purchaseNumber").value(testPurchase.getPurchaseNumber()))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].materialId").value(testMaterial.getId()));
        }

        @Test
        @DisplayName("查詢進貨單詳情 - ID 不存在")
        void getPurchaseDetail_NotFound() throws Exception {
            mockMvc.perform(get("/api/purchases/detail")
                            .param("id", "99999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(3001))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("進貨單不存在: 99999"));
        }
    }

    // ===== GET /api/purchases/list 測試 =====

    @Nested
    @DisplayName("GET /api/purchases/list 測試")
    class ListTests {

        @Test
        @DisplayName("查詢進貨單列表 - 無篩選條件")
        void listPurchases_NoFilter() throws Exception {
            mockMvc.perform(get("/api/purchases/list")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(2));
        }

        @Test
        @DisplayName("查詢進貨單列表 - 關鍵字篩選")
        void listPurchases_WithKeyword() throws Exception {
            mockMvc.perform(get("/api/purchases/list")
                            .param("keyword", "PO-20251205-0001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.content[0].purchaseNumber").value("PO-20251205-0001"));
        }

        @Test
        @DisplayName("查詢進貨單列表 - 狀態篩選")
        void listPurchases_WithStatus() throws Exception {
            mockMvc.perform(get("/api/purchases/list")
                            .param("status", "DRAFT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.content[0].status").value("DRAFT"));
        }

        @Test
        @DisplayName("查詢進貨單列表 - 供應商篩選")
        void listPurchases_WithSupplierId() throws Exception {
            mockMvc.perform(get("/api/purchases/list")
                            .param("supplierId", testSupplier.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.data.totalElements").value(2));
        }

        @Test
        @DisplayName("查詢進貨單列表 - 日期範圍篩選")
        void listPurchases_WithDateRange() throws Exception {
            mockMvc.perform(get("/api/purchases/list")
                            .param("startDate", LocalDate.now().toString())
                            .param("endDate", LocalDate.now().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.data.content").isArray());
        }
    }

    // ===== POST /api/purchases/create 測試 =====

    @Nested
    @DisplayName("POST /api/purchases/create 測試")
    class CreateTests {

        @Test
        @DisplayName("建立進貨單 - 成功")
        void createPurchase_Success() throws Exception {
            CreatePurchaseRequest request = CreatePurchaseRequest.builder()
                    .supplierId(testSupplier.getId())
                    .purchaseDate(LocalDate.now())
                    .items(List.of(CreatePurchaseItemRequest.builder()
                            .materialId(testMaterial.getId())
                            .quantity(new BigDecimal("20.00"))
                            .unitPrice(new BigDecimal("50.00"))
                            .build()))
                    .note("測試進貨單")
                    .build();

            mockMvc.perform(post("/api/purchases/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"))
                    .andExpect(jsonPath("$.data.supplierId").value(testSupplier.getId()))
                    .andExpect(jsonPath("$.data.totalAmount").value(1000.00))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].quantity").value(20.00));
        }

        @Test
        @DisplayName("建立進貨單 - 參數驗證失敗")
        void createPurchase_ValidationError() throws Exception {
            String invalidRequest = "{ \"note\": \"缺少必填欄位\" }";

            mockMvc.perform(post("/api/purchases/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(2001))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data.supplierId").exists());
        }

        @Test
        @DisplayName("建立進貨單 - 供應商不存在")
        void createPurchase_SupplierNotFound() throws Exception {
            CreatePurchaseRequest request = CreatePurchaseRequest.builder()
                    .supplierId(99999L)
                    .purchaseDate(LocalDate.now())
                    .build();

            mockMvc.perform(post("/api/purchases/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(3001))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("供應商不存在: 99999"));
        }
    }

    // ===== POST /api/purchases/update 測試 =====

    @Nested
    @DisplayName("POST /api/purchases/update 測試")
    class UpdateTests {

        @Test
        @DisplayName("更新進貨單 - 成功")
        void updatePurchase_Success() throws Exception {
            UpdatePurchaseRequest request = UpdatePurchaseRequest.builder()
                    .id(testPurchase.getId())
                    .supplierId(testSupplier.getId())
                    .purchaseDate(LocalDate.now())
                    .items(List.of(CreatePurchaseItemRequest.builder()
                            .materialId(testMaterial.getId())
                            .quantity(new BigDecimal("30.00"))
                            .unitPrice(new BigDecimal("80.00"))
                            .build()))
                    .note("更新後的備註")
                    .build();

            mockMvc.perform(post("/api/purchases/update")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.data.note").value("更新後的備註"))
                    .andExpect(jsonPath("$.data.totalAmount").value(2400.00));
        }

        @Test
        @DisplayName("更新進貨單 - 非草稿狀態")
        void updatePurchase_NotDraft() throws Exception {
            UpdatePurchaseRequest request = UpdatePurchaseRequest.builder()
                    .id(confirmedPurchase.getId())
                    .supplierId(testSupplier.getId())
                    .purchaseDate(LocalDate.now())
                    .build();

            mockMvc.perform(post("/api/purchases/update")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(2003))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("只有草稿狀態的進貨單可以操作"));
        }
    }

    // ===== POST /api/purchases/delete 測試 =====

    @Nested
    @DisplayName("POST /api/purchases/delete 測試")
    class DeleteTests {

        @Test
        @DisplayName("刪除進貨單 - 成功")
        void deletePurchase_Success() throws Exception {
            mockMvc.perform(post("/api/purchases/delete")
                            .param("id", testPurchase.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.message").value("進貨單刪除成功"));

            // 驗證已刪除
            assertThat(purchaseRepository.findById(testPurchase.getId())).isEmpty();
        }

        @Test
        @DisplayName("刪除進貨單 - 非草稿狀態")
        void deletePurchase_NotDraft() throws Exception {
            mockMvc.perform(post("/api/purchases/delete")
                            .param("id", confirmedPurchase.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(2003))
                    .andExpect(jsonPath("$.message").value("只有草稿狀態的進貨單可以操作"));

            // 驗證未刪除
            assertThat(purchaseRepository.findById(confirmedPurchase.getId())).isPresent();
        }
    }

    // ===== POST /api/purchases/confirm 測試 =====

    @Nested
    @DisplayName("POST /api/purchases/confirm 測試")
    class ConfirmTests {

        @Test
        @DisplayName("確認進貨單 - 成功")
        void confirmPurchase_Success() throws Exception {
            ConfirmPurchaseRequest request = ConfirmPurchaseRequest.builder()
                    .id(testPurchase.getId())
                    .confirmedBy("測試人員")
                    .build();

            mockMvc.perform(post("/api/purchases/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.data.confirmedBy").value("測試人員"))
                    .andExpect(jsonPath("$.data.confirmedAt").exists());

            // 驗證狀態已更新
            Purchase updatedPurchase = purchaseRepository.findById(testPurchase.getId()).orElseThrow();
            assertThat(updatedPurchase.getStatus()).isEqualTo("CONFIRMED");
            // 注意：庫存更新是異步處理，由 MaterialStockEventListenerTest 單獨測試
        }

        @Test
        @DisplayName("確認進貨單 - 無明細項目")
        void confirmPurchase_NoItems() throws Exception {
            // 建立無明細的進貨單
            Purchase emptyPurchase = purchaseRepository.save(
                    TestDataFactory.defaultPurchase()
                            .purchaseNumber("PO-20251205-0003")
                            .supplierId(testSupplier.getId())
                            .supplierName(testSupplier.getName())
                            .totalAmount(BigDecimal.ZERO)
                            .build()
            );

            ConfirmPurchaseRequest request = ConfirmPurchaseRequest.builder()
                    .id(emptyPurchase.getId())
                    .confirmedBy("測試人員")
                    .build();

            mockMvc.perform(post("/api/purchases/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(2003))
                    .andExpect(jsonPath("$.message").value("進貨單沒有明細，無法確認"));
        }

        @Test
        @DisplayName("確認進貨單 - 非草稿狀態")
        void confirmPurchase_NotDraft() throws Exception {
            ConfirmPurchaseRequest request = ConfirmPurchaseRequest.builder()
                    .id(confirmedPurchase.getId())
                    .confirmedBy("測試人員")
                    .build();

            mockMvc.perform(post("/api/purchases/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(2003))
                    .andExpect(jsonPath("$.message").value("只有草稿狀態的進貨單可以操作"));
        }
    }
}
