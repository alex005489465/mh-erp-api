package com.morningharvest.erp.combo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morningharvest.erp.combo.dto.BatchCreateComboItemRequest;
import com.morningharvest.erp.combo.dto.CreateComboItemRequest;
import com.morningharvest.erp.combo.dto.UpdateComboItemRequest;
import com.morningharvest.erp.combo.entity.Combo;
import com.morningharvest.erp.combo.entity.ComboItem;
import com.morningharvest.erp.combo.repository.ComboItemRepository;
import com.morningharvest.erp.combo.repository.ComboRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
@DisplayName("ComboItemController 整合測試")
class ComboItemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComboRepository comboRepository;

    @Autowired
    private ComboItemRepository comboItemRepository;

    @Autowired
    private ProductRepository productRepository;

    private Combo testCombo;
    private Product testProduct1;
    private Product testProduct2;
    private ComboItem testItem;

    @BeforeEach
    void setUp() {
        comboItemRepository.deleteAll();
        comboRepository.deleteAll();

        testProduct1 = Product.builder()
                .name("招牌漢堡")
                .price(new BigDecimal("59.00"))
                .isActive(true)
                .sortOrder(1)
                .build();
        testProduct1 = productRepository.save(testProduct1);

        testProduct2 = Product.builder()
                .name("薯條")
                .price(new BigDecimal("39.00"))
                .isActive(true)
                .sortOrder(2)
                .build();
        testProduct2 = productRepository.save(testProduct2);

        testCombo = Combo.builder()
                .name("超值早餐A")
                .description("漢堡+飲料組合")
                .price(new BigDecimal("79.00"))
                .isActive(true)
                .sortOrder(1)
                .build();
        testCombo = comboRepository.save(testCombo);

        testItem = ComboItem.builder()
                .comboId(testCombo.getId())
                .productId(testProduct1.getId())
                .productName(testProduct1.getName())
                .quantity(1)
                .sortOrder(0)
                .build();
        testItem = comboItemRepository.save(testItem);
    }

    @Test
    @DisplayName("POST /api/combos/items/create - 建立項目成功")
    void createComboItem_Success() throws Exception {
        CreateComboItemRequest request = CreateComboItemRequest.builder()
                .comboId(testCombo.getId())
                .productId(testProduct2.getId())
                .quantity(2)
                .sortOrder(1)
                .build();

        mockMvc.perform(post("/api/combos/items/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.comboId").value(testCombo.getId()))
                .andExpect(jsonPath("$.data.productId").value(testProduct2.getId()))
                .andExpect(jsonPath("$.data.productName").value("薯條"))
                .andExpect(jsonPath("$.data.quantity").value(2));
    }

    @Test
    @DisplayName("POST /api/combos/items/create - 套餐不存在 (code=3001)")
    void createComboItem_ComboNotFound() throws Exception {
        CreateComboItemRequest request = CreateComboItemRequest.builder()
                .comboId(99999L)
                .productId(testProduct2.getId())
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/combos/items/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("套餐不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/combos/items/create - 商品不存在 (code=3001)")
    void createComboItem_ProductNotFound() throws Exception {
        CreateComboItemRequest request = CreateComboItemRequest.builder()
                .comboId(testCombo.getId())
                .productId(99999L)
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/combos/items/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("商品不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/combos/items/create - 重複商品 (code=2002)")
    void createComboItem_DuplicateProduct() throws Exception {
        CreateComboItemRequest request = CreateComboItemRequest.builder()
                .comboId(testCombo.getId())
                .productId(testProduct1.getId())  // 已存在於 testItem
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/combos/items/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("此套餐已包含此商品，請更新數量而非重複新增"));
    }

    @Test
    @DisplayName("POST /api/combos/items/batch-create - 批次新增成功")
    void batchCreateComboItems_Success() throws Exception {
        // 先刪除現有項目
        comboItemRepository.deleteAll();

        // 新增第三個商品
        Product testProduct3 = Product.builder()
                .name("可樂")
                .price(new BigDecimal("25.00"))
                .isActive(true)
                .sortOrder(3)
                .build();
        testProduct3 = productRepository.save(testProduct3);

        BatchCreateComboItemRequest request = BatchCreateComboItemRequest.builder()
                .comboId(testCombo.getId())
                .items(List.of(
                        BatchCreateComboItemRequest.ComboItemInput.builder()
                                .productId(testProduct1.getId())
                                .quantity(1)
                                .sortOrder(0)
                                .build(),
                        BatchCreateComboItemRequest.ComboItemInput.builder()
                                .productId(testProduct2.getId())
                                .quantity(1)
                                .sortOrder(1)
                                .build(),
                        BatchCreateComboItemRequest.ComboItemInput.builder()
                                .productId(testProduct3.getId())
                                .quantity(1)
                                .sortOrder(2)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/combos/items/batch-create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("GET /api/combos/items/list - 查詢項目列表成功")
    void listComboItems_Success() throws Exception {
        // 新增第二個項目
        ComboItem item2 = ComboItem.builder()
                .comboId(testCombo.getId())
                .productId(testProduct2.getId())
                .productName(testProduct2.getName())
                .quantity(1)
                .sortOrder(1)
                .build();
        comboItemRepository.save(item2);

        mockMvc.perform(get("/api/combos/items/list")
                        .param("comboId", testCombo.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].productName").value("招牌漢堡"))
                .andExpect(jsonPath("$.data[1].productName").value("薯條"));
    }

    @Test
    @DisplayName("GET /api/combos/items/list - 套餐不存在 (code=3001)")
    void listComboItems_ComboNotFound() throws Exception {
        mockMvc.perform(get("/api/combos/items/list")
                        .param("comboId", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("套餐不存在: 99999"));
    }

    @Test
    @DisplayName("GET /api/combos/items/detail - 查詢項目詳情成功")
    void getComboItemDetail_Success() throws Exception {
        mockMvc.perform(get("/api/combos/items/detail")
                        .param("id", testItem.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testItem.getId()))
                .andExpect(jsonPath("$.data.productName").value("招牌漢堡"));
    }

    @Test
    @DisplayName("GET /api/combos/items/detail - 項目不存在 (code=3001)")
    void getComboItemDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/combos/items/detail")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("套餐項目不存在: 99999"));
    }

    @Test
    @DisplayName("POST /api/combos/items/update - 更新項目成功")
    void updateComboItem_Success() throws Exception {
        UpdateComboItemRequest request = UpdateComboItemRequest.builder()
                .quantity(3)
                .sortOrder(5)
                .build();

        mockMvc.perform(post("/api/combos/items/update")
                        .param("id", testItem.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.quantity").value(3))
                .andExpect(jsonPath("$.data.sortOrder").value(5));
    }

    @Test
    @DisplayName("POST /api/combos/items/update - 更換商品成功")
    void updateComboItem_ChangeProduct_Success() throws Exception {
        UpdateComboItemRequest request = UpdateComboItemRequest.builder()
                .productId(testProduct2.getId())
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/combos/items/update")
                        .param("id", testItem.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(testProduct2.getId()))
                .andExpect(jsonPath("$.data.productName").value("薯條"));
    }

    @Test
    @DisplayName("POST /api/combos/items/update - 項目不存在 (code=3001)")
    void updateComboItem_NotFound() throws Exception {
        UpdateComboItemRequest request = UpdateComboItemRequest.builder()
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/combos/items/update")
                        .param("id", "99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/combos/items/delete - 刪除項目成功")
    void deleteComboItem_Success() throws Exception {
        mockMvc.perform(post("/api/combos/items/delete")
                        .param("id", testItem.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true));

        // 驗證已刪除
        assertThat(comboItemRepository.findById(testItem.getId())).isEmpty();
    }

    @Test
    @DisplayName("POST /api/combos/items/delete - 項目不存在 (code=3001)")
    void deleteComboItem_NotFound() throws Exception {
        mockMvc.perform(post("/api/combos/items/delete")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false));
    }
}
