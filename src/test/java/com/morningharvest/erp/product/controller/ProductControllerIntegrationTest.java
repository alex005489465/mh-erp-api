package com.morningharvest.erp.product.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.product.dto.CreateProductRequest;
import com.morningharvest.erp.product.dto.UpdateProductRequest;
import com.morningharvest.erp.product.entity.Product;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ProductController 整合測試")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        testProduct = Product.builder()
                .name("測試商品")
                .description("測試說明")
                .price(new BigDecimal("50.00"))
                .imageUrl("http://example.com/test.jpg")
                .isActive(true)
                .sortOrder(1)
                .build();
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("POST /api/products/create - 建立商品成功")
    void createProduct_Success() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("新商品")
                .description("新商品說明")
                .price(new BigDecimal("60.00"))
                .sortOrder(2)
                .build();

        mockMvc.perform(post("/api/products/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("新商品"))
                .andExpect(jsonPath("$.data.price").value(60.00));
    }

    @Test
    @DisplayName("POST /api/products/create - 參數驗證失敗 (code=2001)")
    void createProduct_ValidationError() throws Exception {
        // 缺少必填欄位
        String invalidRequest = "{ \"description\": \"沒有名稱和價格\" }";

        mockMvc.perform(post("/api/products/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.price").exists());
    }

    @Test
    @DisplayName("POST /api/products/create - 名稱重複 (code=2002)")
    void createProduct_DuplicateName() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("測試商品")  // 與 testProduct 同名
                .price(new BigDecimal("60.00"))
                .build();

        mockMvc.perform(post("/api/products/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("商品名稱已存在: 測試商品"));
    }

    @Test
    @DisplayName("GET /api/products/detail - 查詢商品成功")
    void getProductDetail_Success() throws Exception {
        mockMvc.perform(get("/api/products/detail")
                        .param("id", testProduct.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testProduct.getId()))
                .andExpect(jsonPath("$.data.name").value("測試商品"));
    }

    @Test
    @DisplayName("GET /api/products/detail - 商品不存在 (code=3001)")
    void getProductDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/products/detail")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("商品不存在: 99999"));
    }

    @Test
    @DisplayName("GET /api/products/list - 分頁查詢成功")
    void listProducts_Success() throws Exception {
        mockMvc.perform(get("/api/products/list")
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
    @DisplayName("GET /api/products/list - 篩選上架商品")
    void listProducts_FilterByIsActive() throws Exception {
        // 新增一個下架商品
        Product inactiveProduct = Product.builder()
                .name("下架商品")
                .price(new BigDecimal("30.00"))
                .isActive(false)
                .sortOrder(2)
                .build();
        productRepository.save(inactiveProduct);

        // 查詢上架商品
        mockMvc.perform(get("/api/products/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("測試商品"));
    }

    @Test
    @DisplayName("POST /api/products/update - 更新商品成功")
    void updateProduct_Success() throws Exception {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .id(testProduct.getId())
                .name("更新後的商品")
                .description("更新後的說明")
                .price(new BigDecimal("80.00"))
                .sortOrder(5)
                .build();

        mockMvc.perform(post("/api/products/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("更新後的商品"))
                .andExpect(jsonPath("$.data.price").value(80.00));
    }

    @Test
    @DisplayName("POST /api/products/delete - 刪除商品成功")
    void deleteProduct_Success() throws Exception {
        mockMvc.perform(post("/api/products/delete")
                        .param("id", testProduct.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true));

        // 驗證已刪除
        assertThat(productRepository.findById(testProduct.getId())).isEmpty();
    }

    @Test
    @DisplayName("POST /api/products/activate - 上架商品成功")
    void activateProduct_Success() throws Exception {
        // 先下架
        testProduct.setIsActive(false);
        productRepository.save(testProduct);

        mockMvc.perform(post("/api/products/activate")
                        .param("id", testProduct.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("POST /api/products/deactivate - 下架商品成功")
    void deactivateProduct_Success() throws Exception {
        mockMvc.perform(post("/api/products/deactivate")
                        .param("id", testProduct.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }
}
