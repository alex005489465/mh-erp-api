package com.morningharvest.erp.material.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.material.constant.MaterialCategory;
import com.morningharvest.erp.material.constant.MaterialUnit;
import com.morningharvest.erp.material.dto.CreateMaterialRequest;
import com.morningharvest.erp.material.dto.UpdateMaterialRequest;
import com.morningharvest.erp.material.entity.Material;
import com.morningharvest.erp.material.repository.MaterialRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("MaterialController 整合測試")
class MaterialControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MaterialRepository materialRepository;

    private Material testMaterial;

    @BeforeEach
    void setUp() {
        materialRepository.deleteAll();

        testMaterial = Material.builder()
                .code("M001")
                .name("測試原物料")
                .unit(MaterialUnit.PIECE)
                .category(MaterialCategory.OTHER)
                .specification("測試規格")
                .safeStockQuantity(BigDecimal.TEN)
                .currentStockQuantity(new BigDecimal("50.00"))
                .costPrice(new BigDecimal("25.00"))
                .isActive(true)
                .build();
        testMaterial = materialRepository.save(testMaterial);
    }

    // ===== POST /api/materials/create 測試 =====

    @Test
    @DisplayName("POST /api/materials/create - 建立原物料成功")
    void createMaterial_Success() throws Exception {
        CreateMaterialRequest request = CreateMaterialRequest.builder()
                .code("M002")
                .name("新原物料")
                .unit(MaterialUnit.KILOGRAM)
                .category(MaterialCategory.MEAT)
                .specification("新規格")
                .safeStockQuantity(new BigDecimal("20.00"))
                .currentStockQuantity(new BigDecimal("100.00"))
                .costPrice(new BigDecimal("50.00"))
                .build();

        mockMvc.perform(post("/api/materials/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("M002"))
                .andExpect(jsonPath("$.data.name").value("新原物料"))
                .andExpect(jsonPath("$.data.unit").value("KILOGRAM"))
                .andExpect(jsonPath("$.data.unitDisplayName").value("公斤"))
                .andExpect(jsonPath("$.data.category").value("MEAT"))
                .andExpect(jsonPath("$.data.categoryDisplayName").value("肉類"))
                .andExpect(jsonPath("$.data.costPrice").value(50.00));
    }

    @Test
    @DisplayName("POST /api/materials/create - 參數驗證失敗 (code=2001)")
    void createMaterial_ValidationError() throws Exception {
        // 缺少必填欄位
        String invalidRequest = "{ \"specification\": \"沒有編號名稱和單位\" }";

        mockMvc.perform(post("/api/materials/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").exists())
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.unit").exists());
    }

    @Test
    @DisplayName("POST /api/materials/create - 編號重複 (code=2002)")
    void createMaterial_DuplicateCode() throws Exception {
        CreateMaterialRequest request = CreateMaterialRequest.builder()
                .code("M001")  // 與 testMaterial 同編號
                .name("另一個原物料")
                .unit(MaterialUnit.PACK)
                .build();

        mockMvc.perform(post("/api/materials/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("原物料編號已存在: M001"));
    }

    @Test
    @DisplayName("POST /api/materials/create - 名稱重複 (code=2002)")
    void createMaterial_DuplicateName() throws Exception {
        CreateMaterialRequest request = CreateMaterialRequest.builder()
                .code("M999")
                .name("測試原物料")  // 與 testMaterial 同名
                .unit(MaterialUnit.PACK)
                .build();

        mockMvc.perform(post("/api/materials/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("原物料名稱已存在: 測試原物料"));
    }

    @Test
    @DisplayName("POST /api/materials/create - 無效單位 (code=2002)")
    void createMaterial_InvalidUnit() throws Exception {
        CreateMaterialRequest request = CreateMaterialRequest.builder()
                .code("M003")
                .name("新原物料")
                .unit("INVALID_UNIT")
                .build();

        mockMvc.perform(post("/api/materials/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("無效的單位")));
    }

    // ===== GET /api/materials/detail 測試 =====

    @Test
    @DisplayName("GET /api/materials/detail - 查詢原物料成功")
    void getMaterialDetail_Success() throws Exception {
        mockMvc.perform(get("/api/materials/detail")
                        .param("id", testMaterial.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testMaterial.getId()))
                .andExpect(jsonPath("$.data.code").value("M001"))
                .andExpect(jsonPath("$.data.name").value("測試原物料"))
                .andExpect(jsonPath("$.data.unitDisplayName").value("個"));
    }

    @Test
    @DisplayName("GET /api/materials/detail - 原物料不存在 (code=3001)")
    void getMaterialDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/materials/detail")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("原物料不存在: 99999"));
    }

    // ===== GET /api/materials/list 測試 =====

    @Test
    @DisplayName("GET /api/materials/list - 分頁查詢成功")
    void listMaterials_Success() throws Exception {
        mockMvc.perform(get("/api/materials/list")
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
    @DisplayName("GET /api/materials/list - 篩選分類")
    void listMaterials_FilterByCategory() throws Exception {
        // 新增另一個分類的原物料
        Material meatMaterial = Material.builder()
                .code("M003")
                .name("肉類原物料")
                .unit(MaterialUnit.KILOGRAM)
                .category(MaterialCategory.MEAT)
                .isActive(true)
                .build();
        materialRepository.save(meatMaterial);

        mockMvc.perform(get("/api/materials/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("category", MaterialCategory.MEAT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("肉類原物料"));
    }

    @Test
    @DisplayName("GET /api/materials/list - 篩選啟用狀態")
    void listMaterials_FilterByIsActive() throws Exception {
        // 新增一個停用的原物料
        Material inactiveMaterial = Material.builder()
                .code("M004")
                .name("停用原物料")
                .unit(MaterialUnit.PACK)
                .category(MaterialCategory.OTHER)
                .isActive(false)
                .build();
        materialRepository.save(inactiveMaterial);

        // 查詢啟用的原物料
        mockMvc.perform(get("/api/materials/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].code").value("M001"));
    }

    @Test
    @DisplayName("GET /api/materials/list - 關鍵字搜尋")
    void listMaterials_FilterByKeyword() throws Exception {
        // 新增另一個原物料
        Material anotherMaterial = Material.builder()
                .code("M005")
                .name("雞蛋")
                .unit(MaterialUnit.DOZEN)
                .category(MaterialCategory.EGG)
                .isActive(true)
                .build();
        materialRepository.save(anotherMaterial);

        mockMvc.perform(get("/api/materials/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("keyword", "雞蛋"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("雞蛋"));
    }

    // ===== POST /api/materials/update 測試 =====

    @Test
    @DisplayName("POST /api/materials/update - 更新原物料成功")
    void updateMaterial_Success() throws Exception {
        UpdateMaterialRequest request = UpdateMaterialRequest.builder()
                .id(testMaterial.getId())
                .code("M001")
                .name("更新後的原物料")
                .unit(MaterialUnit.PACK)
                .category(MaterialCategory.BREAD)
                .specification("更新後規格")
                .safeStockQuantity(new BigDecimal("30.00"))
                .costPrice(new BigDecimal("80.00"))
                .build();

        mockMvc.perform(post("/api/materials/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("更新後的原物料"))
                .andExpect(jsonPath("$.data.unit").value("PACK"))
                .andExpect(jsonPath("$.data.unitDisplayName").value("包"))
                .andExpect(jsonPath("$.data.costPrice").value(80.00));
    }

    @Test
    @DisplayName("POST /api/materials/update - 原物料不存在 (code=3001)")
    void updateMaterial_NotFound() throws Exception {
        UpdateMaterialRequest request = UpdateMaterialRequest.builder()
                .id(99999L)
                .code("M999")
                .name("不存在的原物料")
                .unit(MaterialUnit.PIECE)
                .build();

        mockMvc.perform(post("/api/materials/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("原物料不存在: 99999"));
    }

    // ===== POST /api/materials/delete 測試 =====

    @Test
    @DisplayName("POST /api/materials/delete - 停用原物料成功")
    void deleteMaterial_Success() throws Exception {
        mockMvc.perform(post("/api/materials/delete")
                        .param("id", testMaterial.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true));

        // 驗證已停用（軟刪除）
        Material updated = materialRepository.findById(testMaterial.getId()).orElseThrow();
        assertThat(updated.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("POST /api/materials/delete - 原物料不存在 (code=3001)")
    void deleteMaterial_NotFound() throws Exception {
        mockMvc.perform(post("/api/materials/delete")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false));
    }

    // ===== POST /api/materials/activate 測試 =====

    @Test
    @DisplayName("POST /api/materials/activate - 啟用原物料成功")
    void activateMaterial_Success() throws Exception {
        // 先停用
        testMaterial.setIsActive(false);
        materialRepository.save(testMaterial);

        mockMvc.perform(post("/api/materials/activate")
                        .param("id", testMaterial.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("POST /api/materials/activate - 原物料不存在 (code=3001)")
    void activateMaterial_NotFound() throws Exception {
        mockMvc.perform(post("/api/materials/activate")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false));
    }
}
