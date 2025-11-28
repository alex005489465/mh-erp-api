package com.morningharvest.erp.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.product.dto.CreateProductOptionValueRequest;
import com.morningharvest.erp.product.dto.UpdateProductOptionValueRequest;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.entity.ProductOptionGroup;
import com.morningharvest.erp.product.entity.ProductOptionValue;
import com.morningharvest.erp.product.repository.ProductOptionGroupRepository;
import com.morningharvest.erp.product.repository.ProductOptionValueRepository;
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
@DisplayName("ProductOptionValueController 整合測試")
class ProductOptionValueControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionGroupRepository groupRepository;

    @Autowired
    private ProductOptionValueRepository valueRepository;

    private Product testProduct;
    private ProductOptionGroup testGroup;
    private ProductOptionValue testValue;

    @BeforeEach
    void setUp() {
        valueRepository.deleteAll();
        groupRepository.deleteAll();

        // 建立測試用產品
        testProduct = Product.builder()
                .name("測試商品")
                .price(new BigDecimal("100.00"))
                .isActive(true)
                .sortOrder(0)
                .build();
        testProduct = productRepository.save(testProduct);

        // 建立測試用選項群組
        testGroup = ProductOptionGroup.builder()
                .productId(testProduct.getId())
                .name("甜度")
                .minSelections(1)
                .maxSelections(1)
                .sortOrder(0)
                .isActive(true)
                .build();
        testGroup = groupRepository.save(testGroup);

        // 建立測試用選項值
        testValue = ProductOptionValue.builder()
                .groupId(testGroup.getId())
                .name("半糖")
                .priceAdjustment(BigDecimal.ZERO)
                .sortOrder(0)
                .isActive(true)
                .build();
        testValue = valueRepository.save(testValue);
    }

    @Test
    @DisplayName("GET /api/products/options/values/list - 查詢群組下選項值成功")
    void listValues_Success() throws Exception {
        mockMvc.perform(get("/api/products/options/values/list")
                        .param("groupId", testGroup.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("半糖"));
    }

    @Test
    @DisplayName("GET /api/products/options/values/list - 群組不存在 (code=3001)")
    void listValues_GroupNotFound() throws Exception {
        mockMvc.perform(get("/api/products/options/values/list")
                        .param("groupId", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("產品選項群組不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/products/options/values/create - 建立選項值成功")
    void createValue_Success() throws Exception {
        CreateProductOptionValueRequest request = CreateProductOptionValueRequest.builder()
                .groupId(testGroup.getId())
                .name("珍珠")
                .priceAdjustment(new BigDecimal("10.00"))
                .sortOrder(1)
                .build();

        mockMvc.perform(post("/api/products/options/values/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("珍珠"))
                .andExpect(jsonPath("$.data.priceAdjustment").value(10.00));
    }

    @Test
    @DisplayName("POST /api/products/options/values/create - 參數驗證失敗 (code=2001)")
    void createValue_ValidationError() throws Exception {
        String invalidRequest = "{ \"groupId\": " + testGroup.getId() + " }";

        mockMvc.perform(post("/api/products/options/values/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    @DisplayName("POST /api/products/options/values/create - 群組不存在 (code=3001)")
    void createValue_GroupNotFound() throws Exception {
        CreateProductOptionValueRequest request = CreateProductOptionValueRequest.builder()
                .groupId(99999L)
                .name("珍珠")
                .priceAdjustment(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(post("/api/products/options/values/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("產品選項群組不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/products/options/values/create - 同群組內名稱重複 (code=2002)")
    void createValue_DuplicateName() throws Exception {
        CreateProductOptionValueRequest request = CreateProductOptionValueRequest.builder()
                .groupId(testGroup.getId())
                .name("半糖")
                .priceAdjustment(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post("/api/products/options/values/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("群組內選項名稱已存在: 半糖"));
    }

    @Test
    @DisplayName("POST /api/products/options/values/update - 更新選項值成功")
    void updateValue_Success() throws Exception {
        UpdateProductOptionValueRequest request = UpdateProductOptionValueRequest.builder()
                .id(testValue.getId())
                .name("更新半糖")
                .priceAdjustment(new BigDecimal("5.00"))
                .sortOrder(2)
                .build();

        mockMvc.perform(post("/api/products/options/values/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("更新半糖"))
                .andExpect(jsonPath("$.data.priceAdjustment").value(5.00));
    }

    @Test
    @DisplayName("POST /api/products/options/values/update - 選項值不存在 (code=3001)")
    void updateValue_NotFound() throws Exception {
        UpdateProductOptionValueRequest request = UpdateProductOptionValueRequest.builder()
                .id(99999L)
                .name("測試")
                .priceAdjustment(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post("/api/products/options/values/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("產品選項值不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/products/options/values/update - 同群組內名稱重複 (code=2002)")
    void updateValue_DuplicateName() throws Exception {
        // 建立另一個選項值
        ProductOptionValue anotherValue = ProductOptionValue.builder()
                .groupId(testGroup.getId())
                .name("全糖")
                .priceAdjustment(BigDecimal.ZERO)
                .sortOrder(1)
                .isActive(true)
                .build();
        anotherValue = valueRepository.save(anotherValue);

        // 嘗試將 testValue 的名稱改成 "全糖" (已存在)
        UpdateProductOptionValueRequest request = UpdateProductOptionValueRequest.builder()
                .id(testValue.getId())
                .name("全糖")
                .priceAdjustment(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post("/api/products/options/values/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("群組內選項名稱已存在: 全糖"));
    }

    @Test
    @DisplayName("POST /api/products/options/values/delete - 刪除選項值成功")
    void deleteValue_Success() throws Exception {
        mockMvc.perform(post("/api/products/options/values/delete")
                        .param("id", testValue.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true));

        assertThat(valueRepository.findById(testValue.getId())).isEmpty();
    }

    @Test
    @DisplayName("POST /api/products/options/values/delete - 選項值不存在 (code=3001)")
    void deleteValue_NotFound() throws Exception {
        mockMvc.perform(post("/api/products/options/values/delete")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("產品選項值不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/products/options/values/activate - 啟用選項值成功")
    void activateValue_Success() throws Exception {
        testValue.setIsActive(false);
        valueRepository.save(testValue);

        mockMvc.perform(post("/api/products/options/values/activate")
                        .param("id", testValue.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("POST /api/products/options/values/deactivate - 停用選項值成功")
    void deactivateValue_Success() throws Exception {
        mockMvc.perform(post("/api/products/options/values/deactivate")
                        .param("id", testValue.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }
}
