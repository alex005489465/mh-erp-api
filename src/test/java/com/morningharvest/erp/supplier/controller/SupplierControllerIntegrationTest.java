package com.morningharvest.erp.supplier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.common.test.TestDataFactory;
import com.morningharvest.erp.supplier.constant.PaymentTerms;
import com.morningharvest.erp.supplier.dto.CreateSupplierRequest;
import com.morningharvest.erp.supplier.dto.UpdateSupplierRequest;
import com.morningharvest.erp.supplier.entity.Supplier;
import com.morningharvest.erp.supplier.repository.SupplierRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SupplierController 整合測試
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("SupplierController 整合測試")
class SupplierControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SupplierRepository supplierRepository;

    private Supplier testSupplier;

    @BeforeEach
    void setUp() {
        supplierRepository.deleteAll();

        testSupplier = TestDataFactory.defaultSupplier().build();
        testSupplier = supplierRepository.save(testSupplier);
    }

    // ===== 建立供應商測試 =====

    @Test
    @DisplayName("POST /api/suppliers/create - 建立供應商成功")
    void createSupplier_Success() throws Exception {
        CreateSupplierRequest request = CreateSupplierRequest.builder()
                .code("S999")
                .name("新供應商")
                .shortName("新供")
                .contactPerson("張三")
                .phone("02-11112222")
                .email("new@example.com")
                .paymentTerms(PaymentTerms.NET60)
                .build();

        mockMvc.perform(post("/api/suppliers/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("S999"))
                .andExpect(jsonPath("$.data.name").value("新供應商"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("POST /api/suppliers/create - 缺少必填欄位，回應 code=2001")
    void createSupplier_ValidationError() throws Exception {
        String invalidRequest = "{ \"shortName\": \"無編號無名稱\" }";

        mockMvc.perform(post("/api/suppliers/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/suppliers/create - 編號重複，回應 code=2002")
    void createSupplier_DuplicateCode() throws Exception {
        CreateSupplierRequest request = CreateSupplierRequest.builder()
                .code("S001")  // 已存在的編號
                .name("重複編號供應商")
                .build();

        mockMvc.perform(post("/api/suppliers/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("供應商編號已存在")));
    }

    // ===== 更新供應商測試 =====

    @Test
    @DisplayName("POST /api/suppliers/update - 更新供應商成功")
    void updateSupplier_Success() throws Exception {
        UpdateSupplierRequest request = UpdateSupplierRequest.builder()
                .id(testSupplier.getId())
                .code("S001")
                .name("更新後供應商")
                .shortName("更新")
                .contactPerson("李四")
                .paymentTerms(PaymentTerms.NET90)
                .build();

        mockMvc.perform(post("/api/suppliers/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("更新後供應商"))
                .andExpect(jsonPath("$.data.contactPerson").value("李四"));
    }

    @Test
    @DisplayName("POST /api/suppliers/update - 供應商不存在，回應 code=3001")
    void updateSupplier_NotFound() throws Exception {
        UpdateSupplierRequest request = UpdateSupplierRequest.builder()
                .id(99999L)
                .code("S999")
                .name("不存在供應商")
                .build();

        mockMvc.perform(post("/api/suppliers/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false));
    }

    // ===== 查詢供應商測試 =====

    @Test
    @DisplayName("GET /api/suppliers/detail - 查詢供應商詳情成功")
    void getSupplierDetail_Success() throws Exception {
        mockMvc.perform(get("/api/suppliers/detail")
                        .param("id", testSupplier.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testSupplier.getId()))
                .andExpect(jsonPath("$.data.code").value("S001"))
                .andExpect(jsonPath("$.data.paymentTermsDisplayName").value("30 天付款"));
    }

    @Test
    @DisplayName("GET /api/suppliers/detail - 供應商不存在，回應 code=3001")
    void getSupplierDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/suppliers/detail")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/suppliers/list - 分頁查詢成功")
    void listSuppliers_Success() throws Exception {
        mockMvc.perform(get("/api/suppliers/list")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/suppliers/list - 帶篩選條件查詢")
    void listSuppliers_WithFilters() throws Exception {
        // 新增停用的供應商
        Supplier inactiveSupplier = TestDataFactory.inactiveSupplier().build();
        supplierRepository.save(inactiveSupplier);

        // 篩選啟用的供應商
        mockMvc.perform(get("/api/suppliers/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        // 關鍵字搜尋
        mockMvc.perform(get("/api/suppliers/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("keyword", "測試"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.content[0].name").value(org.hamcrest.Matchers.containsString("測試")));
    }

    // ===== 刪除供應商測試 =====

    @Test
    @DisplayName("POST /api/suppliers/delete - 刪除供應商成功")
    void deleteSupplier_Success() throws Exception {
        mockMvc.perform(post("/api/suppliers/delete")
                        .param("id", testSupplier.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true));

        // 驗證已刪除
        assertThat(supplierRepository.findById(testSupplier.getId())).isEmpty();
    }

    // ===== 啟用/停用供應商測試 =====

    @Test
    @DisplayName("POST /api/suppliers/activate - 啟用供應商成功")
    void activateSupplier_Success() throws Exception {
        // 先建立停用的供應商
        Supplier inactiveSupplier = TestDataFactory.inactiveSupplier().build();
        inactiveSupplier = supplierRepository.save(inactiveSupplier);

        mockMvc.perform(post("/api/suppliers/activate")
                        .param("id", inactiveSupplier.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("POST /api/suppliers/deactivate - 停用供應商成功")
    void deactivateSupplier_Success() throws Exception {
        mockMvc.perform(post("/api/suppliers/deactivate")
                        .param("id", testSupplier.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }
}
