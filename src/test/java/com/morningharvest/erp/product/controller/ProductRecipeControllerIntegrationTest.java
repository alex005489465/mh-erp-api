package com.morningharvest.erp.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.common.test.TestDataFactory;
import com.morningharvest.erp.material.entity.Material;
import com.morningharvest.erp.material.repository.MaterialRepository;
import com.morningharvest.erp.product.dto.CreateProductRecipeRequest;
import com.morningharvest.erp.product.dto.UpdateProductRecipeRequest;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.entity.ProductRecipe;
import com.morningharvest.erp.product.repository.ProductRecipeRepository;
import com.morningharvest.erp.product.repository.ProductRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ProductRecipeController 整合測試")
class ProductRecipeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRecipeRepository productRecipeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MaterialRepository materialRepository;

    private Product testProduct;
    private Material testMaterial;
    private ProductRecipe testRecipe;

    @BeforeEach
    void setUp() {
        productRecipeRepository.deleteAll();
        productRepository.deleteAll();
        materialRepository.deleteAll();

        // 建立測試商品
        testProduct = TestDataFactory.defaultProduct().build();
        testProduct = productRepository.save(testProduct);

        // 建立測試原物料
        testMaterial = TestDataFactory.defaultMaterial().build();
        testMaterial = materialRepository.save(testMaterial);

        // 建立測試配方
        testRecipe = ProductRecipe.builder()
                .productId(testProduct.getId())
                .productName(testProduct.getName())
                .materialId(testMaterial.getId())
                .materialCode(testMaterial.getCode())
                .materialName(testMaterial.getName())
                .quantity(new BigDecimal("1.0000"))
                .unit(testMaterial.getUnit())
                .note("測試備註")
                .build();
        testRecipe = productRecipeRepository.save(testRecipe);
    }

    // ===== POST /api/products/recipes/create 測試 =====

    @Test
    @DisplayName("POST /api/products/recipes/create - 新增配方成功")
    void createRecipe_Success() throws Exception {
        // 建立另一個原物料
        Material anotherMaterial = Material.builder()
                .code("M002")
                .name("另一個原物料")
                .unit("KILOGRAM")
                .category("OTHER")
                .isActive(true)
                .build();
        anotherMaterial = materialRepository.save(anotherMaterial);

        CreateProductRecipeRequest request = CreateProductRecipeRequest.builder()
                .productId(testProduct.getId())
                .materialId(anotherMaterial.getId())
                .quantity(new BigDecimal("2.5000"))
                .note("新配方")
                .build();

        mockMvc.perform(post("/api/products/recipes/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(testProduct.getId()))
                .andExpect(jsonPath("$.data.productName").value(testProduct.getName()))
                .andExpect(jsonPath("$.data.materialId").value(anotherMaterial.getId()))
                .andExpect(jsonPath("$.data.materialCode").value("M002"))
                .andExpect(jsonPath("$.data.quantity").value(2.5));
    }

    @Test
    @DisplayName("POST /api/products/recipes/create - 參數驗證失敗 (code=2001)")
    void createRecipe_ValidationError() throws Exception {
        // 缺少必填欄位
        String invalidRequest = "{ \"note\": \"只有備註\" }";

        mockMvc.perform(post("/api/products/recipes/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.productId").exists())
                .andExpect(jsonPath("$.data.materialId").exists())
                .andExpect(jsonPath("$.data.quantity").exists());
    }

    @Test
    @DisplayName("POST /api/products/recipes/create - 商品不存在 (code=3001)")
    void createRecipe_ProductNotFound() throws Exception {
        CreateProductRecipeRequest request = CreateProductRecipeRequest.builder()
                .productId(99999L)
                .materialId(testMaterial.getId())
                .quantity(new BigDecimal("1.0000"))
                .build();

        mockMvc.perform(post("/api/products/recipes/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("商品不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/products/recipes/create - 原物料不存在 (code=3001)")
    void createRecipe_MaterialNotFound() throws Exception {
        CreateProductRecipeRequest request = CreateProductRecipeRequest.builder()
                .productId(testProduct.getId())
                .materialId(99999L)
                .quantity(new BigDecimal("1.0000"))
                .build();

        mockMvc.perform(post("/api/products/recipes/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("原物料不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/products/recipes/create - 重複配方 (code=2002)")
    void createRecipe_Duplicate() throws Exception {
        // 嘗試新增已存在的配方
        CreateProductRecipeRequest request = CreateProductRecipeRequest.builder()
                .productId(testProduct.getId())
                .materialId(testMaterial.getId())
                .quantity(new BigDecimal("1.0000"))
                .build();

        mockMvc.perform(post("/api/products/recipes/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("此商品已有該原物料的配方"));
    }

    // ===== POST /api/products/recipes/update 測試 =====

    @Test
    @DisplayName("POST /api/products/recipes/update - 更新配方成功")
    void updateRecipe_Success() throws Exception {
        UpdateProductRecipeRequest request = UpdateProductRecipeRequest.builder()
                .id(testRecipe.getId())
                .quantity(new BigDecimal("5.0000"))
                .note("更新後備註")
                .build();

        mockMvc.perform(post("/api/products/recipes/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.quantity").value(5.0))
                .andExpect(jsonPath("$.data.note").value("更新後備註"));
    }

    @Test
    @DisplayName("POST /api/products/recipes/update - 配方不存在 (code=3001)")
    void updateRecipe_NotFound() throws Exception {
        UpdateProductRecipeRequest request = UpdateProductRecipeRequest.builder()
                .id(99999L)
                .quantity(new BigDecimal("1.0000"))
                .build();

        mockMvc.perform(post("/api/products/recipes/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("配方不存在: 99999"));
    }

    // ===== POST /api/products/recipes/delete 測試 =====

    @Test
    @DisplayName("POST /api/products/recipes/delete - 刪除配方成功")
    void deleteRecipe_Success() throws Exception {
        mockMvc.perform(post("/api/products/recipes/delete")
                        .param("id", testRecipe.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/products/recipes/delete - 配方不存在 (code=3001)")
    void deleteRecipe_NotFound() throws Exception {
        mockMvc.perform(post("/api/products/recipes/delete")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false));
    }

    // ===== GET /api/products/recipes/detail 測試 =====

    @Test
    @DisplayName("GET /api/products/recipes/detail - 查詢配方成功")
    void getRecipeDetail_Success() throws Exception {
        mockMvc.perform(get("/api/products/recipes/detail")
                        .param("id", testRecipe.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testRecipe.getId()))
                .andExpect(jsonPath("$.data.productId").value(testProduct.getId()))
                .andExpect(jsonPath("$.data.materialId").value(testMaterial.getId()));
    }

    @Test
    @DisplayName("GET /api/products/recipes/detail - 配方不存在 (code=3001)")
    void getRecipeDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/products/recipes/detail")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("配方不存在: 99999"));
    }

    // ===== GET /api/products/recipes/list 測試 =====

    @Test
    @DisplayName("GET /api/products/recipes/list - 查詢配方清單成功")
    void listRecipes_Success() throws Exception {
        mockMvc.perform(get("/api/products/recipes/list")
                        .param("productId", testProduct.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].productId").value(testProduct.getId()));
    }

    @Test
    @DisplayName("GET /api/products/recipes/list - 空清單")
    void listRecipes_EmptyList() throws Exception {
        // 建立一個沒有配方的新商品
        Product newProduct = Product.builder()
                .name("無配方商品")
                .price(new BigDecimal("100.00"))
                .isActive(true)
                .build();
        newProduct = productRepository.save(newProduct);

        mockMvc.perform(get("/api/products/recipes/list")
                        .param("productId", newProduct.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("GET /api/products/recipes/list - 商品不存在 (code=3001)")
    void listRecipes_ProductNotFound() throws Exception {
        mockMvc.perform(get("/api/products/recipes/list")
                        .param("productId", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("商品不存在: 99999"));
    }
}
