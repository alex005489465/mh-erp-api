package com.morningharvest.erp.combo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.combo.dto.CreateComboRequest;
import com.morningharvest.erp.combo.dto.UpdateComboRequest;
import com.morningharvest.erp.combo.entity.Combo;
import com.morningharvest.erp.combo.entity.ComboItem;
import com.morningharvest.erp.combo.repository.ComboItemRepository;
import com.morningharvest.erp.combo.repository.ComboRepository;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.entity.ProductCategory;
import com.morningharvest.erp.product.repository.ProductCategoryRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ComboController 整合測試")
class ComboControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComboRepository comboRepository;

    @Autowired
    private ComboItemRepository comboItemRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Combo testCombo;
    private ProductCategory testCategory;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        comboItemRepository.deleteAll();
        comboRepository.deleteAll();

        testCategory = ProductCategory.builder()
                .name("早餐套餐")
                .isActive(true)
                .sortOrder(1)
                .build();
        testCategory = productCategoryRepository.save(testCategory);

        testProduct = Product.builder()
                .name("招牌漢堡")
                .price(new BigDecimal("59.00"))
                .isActive(true)
                .sortOrder(1)
                .build();
        testProduct = productRepository.save(testProduct);

        testCombo = Combo.builder()
                .name("超值早餐A")
                .description("漢堡+飲料組合")
                .price(new BigDecimal("79.00"))
                .imageUrl("http://example.com/combo.jpg")
                .categoryId(testCategory.getId())
                .categoryName(testCategory.getName())
                .isActive(true)
                .sortOrder(1)
                .build();
        testCombo = comboRepository.save(testCombo);
    }

    @Test
    @DisplayName("POST /api/combos/create - 建立套餐成功")
    void createCombo_Success() throws Exception {
        CreateComboRequest request = CreateComboRequest.builder()
                .name("新套餐")
                .description("新套餐說明")
                .price(new BigDecimal("99.00"))
                .categoryId(testCategory.getId())
                .sortOrder(2)
                .build();

        mockMvc.perform(post("/api/combos/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("新套餐"))
                .andExpect(jsonPath("$.data.price").value(99.00))
                .andExpect(jsonPath("$.data.categoryName").value("早餐套餐"));
    }

    @Test
    @DisplayName("POST /api/combos/create - 參數驗證失敗 (code=2001)")
    void createCombo_ValidationError() throws Exception {
        String invalidRequest = "{ \"description\": \"沒有名稱和價格\" }";

        mockMvc.perform(post("/api/combos/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.price").exists());
    }

    @Test
    @DisplayName("POST /api/combos/create - 名稱重複 (code=2002)")
    void createCombo_DuplicateName() throws Exception {
        CreateComboRequest request = CreateComboRequest.builder()
                .name("超值早餐A")  // 與 testCombo 同名
                .price(new BigDecimal("99.00"))
                .build();

        mockMvc.perform(post("/api/combos/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("套餐名稱已存在: 超值早餐A"));
    }

    @Test
    @DisplayName("POST /api/combos/create - 分類不存在 (code=3001)")
    void createCombo_CategoryNotFound() throws Exception {
        CreateComboRequest request = CreateComboRequest.builder()
                .name("新套餐")
                .price(new BigDecimal("99.00"))
                .categoryId(99999L)
                .build();

        mockMvc.perform(post("/api/combos/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("商品分類不存在: 99999"));
    }

    @Test
    @DisplayName("GET /api/combos/detail - 查詢套餐成功")
    void getComboDetail_Success() throws Exception {
        mockMvc.perform(get("/api/combos/detail")
                        .param("id", testCombo.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testCombo.getId()))
                .andExpect(jsonPath("$.data.name").value("超值早餐A"))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("GET /api/combos/detail - 套餐不存在 (code=3001)")
    void getComboDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/combos/detail")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("套餐不存在: 99999"));
    }

    @Test
    @DisplayName("GET /api/combos/list - 分頁查詢成功")
    void listCombos_Success() throws Exception {
        mockMvc.perform(get("/api/combos/list")
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
    @DisplayName("GET /api/combos/list - 篩選啟用套餐")
    void listCombos_FilterByIsActive() throws Exception {
        Combo inactiveCombo = Combo.builder()
                .name("下架套餐")
                .price(new BigDecimal("60.00"))
                .isActive(false)
                .sortOrder(2)
                .build();
        comboRepository.save(inactiveCombo);

        mockMvc.perform(get("/api/combos/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("超值早餐A"));
    }

    @Test
    @DisplayName("GET /api/combos/list - 依分類篩選")
    void listCombos_FilterByCategory() throws Exception {
        ProductCategory otherCategory = ProductCategory.builder()
                .name("午餐套餐")
                .isActive(true)
                .sortOrder(2)
                .build();
        otherCategory = productCategoryRepository.save(otherCategory);

        Combo otherCombo = Combo.builder()
                .name("午餐套餐A")
                .price(new BigDecimal("120.00"))
                .categoryId(otherCategory.getId())
                .categoryName(otherCategory.getName())
                .isActive(true)
                .sortOrder(2)
                .build();
        comboRepository.save(otherCombo);

        mockMvc.perform(get("/api/combos/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("categoryId", testCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("超值早餐A"));
    }

    @Test
    @DisplayName("POST /api/combos/update - 更新套餐成功")
    void updateCombo_Success() throws Exception {
        UpdateComboRequest request = UpdateComboRequest.builder()
                .name("更新後的套餐")
                .description("更新後的說明")
                .price(new BigDecimal("89.00"))
                .sortOrder(5)
                .build();

        mockMvc.perform(post("/api/combos/update")
                        .param("id", testCombo.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("更新後的套餐"))
                .andExpect(jsonPath("$.data.price").value(89.00));
    }

    @Test
    @DisplayName("POST /api/combos/update - 套餐不存在 (code=3001)")
    void updateCombo_NotFound() throws Exception {
        UpdateComboRequest request = UpdateComboRequest.builder()
                .name("更新後的套餐")
                .price(new BigDecimal("89.00"))
                .build();

        mockMvc.perform(post("/api/combos/update")
                        .param("id", "99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/combos/delete - 刪除套餐成功")
    void deleteCombo_Success() throws Exception {
        // 先新增一個項目
        ComboItem item = ComboItem.builder()
                .comboId(testCombo.getId())
                .productId(testProduct.getId())
                .productName(testProduct.getName())
                .quantity(1)
                .sortOrder(0)
                .build();
        comboItemRepository.save(item);

        mockMvc.perform(post("/api/combos/delete")
                        .param("id", testCombo.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true));

        // 驗證套餐和項目都已刪除
        assertThat(comboRepository.findById(testCombo.getId())).isEmpty();
        assertThat(comboItemRepository.findByComboIdOrderBySortOrder(testCombo.getId())).isEmpty();
    }

    @Test
    @DisplayName("POST /api/combos/delete - 套餐不存在 (code=3001)")
    void deleteCombo_NotFound() throws Exception {
        mockMvc.perform(post("/api/combos/delete")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/combos/activate - 啟用套餐成功")
    void activateCombo_Success() throws Exception {
        testCombo.setIsActive(false);
        comboRepository.save(testCombo);

        mockMvc.perform(post("/api/combos/activate")
                        .param("id", testCombo.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("POST /api/combos/deactivate - 停用套餐成功")
    void deactivateCombo_Success() throws Exception {
        mockMvc.perform(post("/api/combos/deactivate")
                        .param("id", testCombo.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }
}
