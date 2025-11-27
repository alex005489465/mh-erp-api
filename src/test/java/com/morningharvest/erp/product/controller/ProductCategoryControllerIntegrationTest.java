package com.morningharvest.erp.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.product.dto.CreateProductCategoryRequest;
import com.morningharvest.erp.product.dto.UpdateProductCategoryRequest;
import com.morningharvest.erp.product.entity.ProductCategory;
import com.morningharvest.erp.product.repository.ProductCategoryRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ProductCategoryController 整合測試")
class ProductCategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    private ProductCategory testCategory;

    @BeforeEach
    void setUp() {
        productCategoryRepository.deleteAll();

        testCategory = ProductCategory.builder()
                .name("測試分類")
                .description("測試說明")
                .isActive(true)
                .sortOrder(1)
                .build();
        testCategory = productCategoryRepository.save(testCategory);
    }

    @Test
    @DisplayName("POST /api/products/categories/create - 建立分類成功")
    void createCategory_Success() throws Exception {
        CreateProductCategoryRequest request = CreateProductCategoryRequest.builder()
                .name("新分類")
                .description("新分類說明")
                .sortOrder(2)
                .build();

        mockMvc.perform(post("/api/products/categories/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("新分類"));
    }

    @Test
    @DisplayName("POST /api/products/categories/create - 參數驗證失敗 (code=2001)")
    void createCategory_ValidationError() throws Exception {
        // 缺少必填欄位
        String invalidRequest = "{ \"description\": \"沒有名稱\" }";

        mockMvc.perform(post("/api/products/categories/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    @DisplayName("POST /api/products/categories/create - 名稱重複 (code=2002)")
    void createCategory_DuplicateName() throws Exception {
        CreateProductCategoryRequest request = CreateProductCategoryRequest.builder()
                .name("測試分類")  // 與 testCategory 同名
                .build();

        mockMvc.perform(post("/api/products/categories/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("分類名稱已存在: 測試分類"));
    }

    @Test
    @DisplayName("GET /api/products/categories/detail - 查詢分類成功")
    void getCategoryDetail_Success() throws Exception {
        mockMvc.perform(get("/api/products/categories/detail")
                        .param("id", testCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testCategory.getId()))
                .andExpect(jsonPath("$.data.name").value("測試分類"));
    }

    @Test
    @DisplayName("GET /api/products/categories/detail - 分類不存在 (code=3001)")
    void getCategoryDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/products/categories/detail")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("商品分類不存在: 99999"));
    }

    @Test
    @DisplayName("GET /api/products/categories/list - 分頁查詢成功")
    void listCategories_Success() throws Exception {
        mockMvc.perform(get("/api/products/categories/list")
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
    @DisplayName("GET /api/products/categories/list - 篩選啟用分類")
    void listCategories_FilterByIsActive() throws Exception {
        // 新增一個停用分類
        ProductCategory inactiveCategory = ProductCategory.builder()
                .name("停用分類")
                .isActive(false)
                .sortOrder(2)
                .build();
        productCategoryRepository.save(inactiveCategory);

        // 查詢啟用分類
        mockMvc.perform(get("/api/products/categories/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("測試分類"));
    }

    @Test
    @DisplayName("POST /api/products/categories/update - 更新分類成功")
    void updateCategory_Success() throws Exception {
        UpdateProductCategoryRequest request = UpdateProductCategoryRequest.builder()
                .id(testCategory.getId())
                .name("更新後的分類")
                .description("更新後的說明")
                .sortOrder(5)
                .build();

        mockMvc.perform(post("/api/products/categories/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("更新後的分類"));
    }

    @Test
    @DisplayName("POST /api/products/categories/delete - 刪除分類成功")
    void deleteCategory_Success() throws Exception {
        mockMvc.perform(post("/api/products/categories/delete")
                        .param("id", testCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true));

        // 驗證已刪除
        assertThat(productCategoryRepository.findById(testCategory.getId())).isEmpty();
    }

    @Test
    @DisplayName("POST /api/products/categories/activate - 啟用分類成功")
    void activateCategory_Success() throws Exception {
        // 先停用
        testCategory.setIsActive(false);
        productCategoryRepository.save(testCategory);

        mockMvc.perform(post("/api/products/categories/activate")
                        .param("id", testCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("POST /api/products/categories/deactivate - 停用分類成功")
    void deactivateCategory_Success() throws Exception {
        mockMvc.perform(post("/api/products/categories/deactivate")
                        .param("id", testCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }
}
