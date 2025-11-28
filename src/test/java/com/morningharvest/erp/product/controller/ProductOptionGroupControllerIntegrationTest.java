package com.morningharvest.erp.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.product.dto.CreateProductOptionGroupRequest;
import com.morningharvest.erp.product.dto.UpdateProductOptionGroupRequest;
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
@DisplayName("ProductOptionGroupController 整合測試")
class ProductOptionGroupControllerIntegrationTest {

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
    @DisplayName("POST /api/products/options/groups/create - 建立群組成功")
    void createGroup_Success() throws Exception {
        CreateProductOptionGroupRequest request = CreateProductOptionGroupRequest.builder()
                .productId(testProduct.getId())
                .name("加料")
                .minSelections(0)
                .maxSelections(3)
                .sortOrder(1)
                .build();

        mockMvc.perform(post("/api/products/options/groups/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(testProduct.getId()))
                .andExpect(jsonPath("$.data.name").value("加料"))
                .andExpect(jsonPath("$.data.minSelections").value(0))
                .andExpect(jsonPath("$.data.maxSelections").value(3));
    }

    @Test
    @DisplayName("POST /api/products/options/groups/create - 參數驗證失敗 (code=2001)")
    void createGroup_ValidationError() throws Exception {
        String invalidRequest = "{ \"productId\": " + testProduct.getId() + ", \"minSelections\": 1, \"maxSelections\": 1 }";

        mockMvc.perform(post("/api/products/options/groups/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    @DisplayName("POST /api/products/options/groups/create - 產品不存在 (code=3001)")
    void createGroup_ProductNotFound() throws Exception {
        CreateProductOptionGroupRequest request = CreateProductOptionGroupRequest.builder()
                .productId(99999L)
                .name("加料")
                .minSelections(0)
                .maxSelections(3)
                .build();

        mockMvc.perform(post("/api/products/options/groups/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("產品不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/products/options/groups/create - 名稱重複 (code=2002)")
    void createGroup_DuplicateName() throws Exception {
        CreateProductOptionGroupRequest request = CreateProductOptionGroupRequest.builder()
                .productId(testProduct.getId())
                .name("甜度")
                .minSelections(1)
                .maxSelections(1)
                .build();

        mockMvc.perform(post("/api/products/options/groups/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("此產品已有相同名稱的選項群組: 甜度"));
    }

    @Test
    @DisplayName("POST /api/products/options/groups/create - minSelections > maxSelections (code=2002)")
    void createGroup_InvalidSelections() throws Exception {
        CreateProductOptionGroupRequest request = CreateProductOptionGroupRequest.builder()
                .productId(testProduct.getId())
                .name("測試")
                .minSelections(5)
                .maxSelections(1)
                .build();

        mockMvc.perform(post("/api/products/options/groups/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("最少選擇數不可大於最多選擇數"));
    }

    @Test
    @DisplayName("GET /api/products/options/groups/detail - 查詢群組成功（含選項值）")
    void getGroupDetail_Success() throws Exception {
        mockMvc.perform(get("/api/products/options/groups/detail")
                        .param("id", testGroup.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testGroup.getId()))
                .andExpect(jsonPath("$.data.productId").value(testProduct.getId()))
                .andExpect(jsonPath("$.data.name").value("甜度"))
                .andExpect(jsonPath("$.data.values").isArray())
                .andExpect(jsonPath("$.data.values[0].name").value("半糖"));
    }

    @Test
    @DisplayName("GET /api/products/options/groups/detail - 群組不存在 (code=3001)")
    void getGroupDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/products/options/groups/detail")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("產品選項群組不存在: 99999"));
    }

    @Test
    @DisplayName("GET /api/products/options/groups/list - 查詢產品選項群組列表成功")
    void listGroups_Success() throws Exception {
        mockMvc.perform(get("/api/products/options/groups/list")
                        .param("productId", testProduct.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("甜度"));
    }

    @Test
    @DisplayName("GET /api/products/options/groups/list - 產品不存在 (code=3001)")
    void listGroups_ProductNotFound() throws Exception {
        mockMvc.perform(get("/api/products/options/groups/list")
                        .param("productId", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("產品不存在: 99999"));
    }

    @Test
    @DisplayName("GET /api/products/options/groups/list-with-values - 查詢群組列表（含選項值）成功")
    void listGroupsWithValues_Success() throws Exception {
        mockMvc.perform(get("/api/products/options/groups/list-with-values")
                        .param("productId", testProduct.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("甜度"))
                .andExpect(jsonPath("$.data[0].values").isArray())
                .andExpect(jsonPath("$.data[0].values[0].name").value("半糖"));
    }

    @Test
    @DisplayName("POST /api/products/options/groups/update - 更新群組成功")
    void updateGroup_Success() throws Exception {
        UpdateProductOptionGroupRequest request = UpdateProductOptionGroupRequest.builder()
                .id(testGroup.getId())
                .name("更新甜度")
                .minSelections(0)
                .maxSelections(1)
                .sortOrder(5)
                .build();

        mockMvc.perform(post("/api/products/options/groups/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("更新甜度"));
    }

    @Test
    @DisplayName("POST /api/products/options/groups/delete - 刪除群組成功（級聯刪除選項值）")
    void deleteGroup_Success() throws Exception {
        mockMvc.perform(post("/api/products/options/groups/delete")
                        .param("id", testGroup.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true));

        assertThat(groupRepository.findById(testGroup.getId())).isEmpty();
        assertThat(valueRepository.findByGroupIdOrderBySortOrder(testGroup.getId())).isEmpty();
    }

    @Test
    @DisplayName("POST /api/products/options/groups/activate - 啟用群組成功")
    void activateGroup_Success() throws Exception {
        testGroup.setIsActive(false);
        groupRepository.save(testGroup);

        mockMvc.perform(post("/api/products/options/groups/activate")
                        .param("id", testGroup.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("POST /api/products/options/groups/deactivate - 停用群組成功")
    void deactivateGroup_Success() throws Exception {
        mockMvc.perform(post("/api/products/options/groups/deactivate")
                        .param("id", testGroup.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }
}
