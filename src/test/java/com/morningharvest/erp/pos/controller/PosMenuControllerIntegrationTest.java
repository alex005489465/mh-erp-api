package com.morningharvest.erp.pos.controller;

import com.morningharvest.erp.combo.entity.Combo;
import com.morningharvest.erp.combo.entity.ComboItem;
import com.morningharvest.erp.combo.repository.ComboItemRepository;
import com.morningharvest.erp.combo.repository.ComboRepository;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.entity.ProductCategory;
import com.morningharvest.erp.product.entity.ProductOptionGroup;
import com.morningharvest.erp.product.entity.ProductOptionValue;
import com.morningharvest.erp.product.repository.ProductCategoryRepository;
import com.morningharvest.erp.product.repository.ProductOptionGroupRepository;
import com.morningharvest.erp.product.repository.ProductOptionValueRepository;
import com.morningharvest.erp.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("PosMenuController 整合測試")
class PosMenuControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private ProductOptionGroupRepository optionGroupRepository;

    @Autowired
    private ProductOptionValueRepository optionValueRepository;

    @Autowired
    private ComboRepository comboRepository;

    @Autowired
    private ComboItemRepository comboItemRepository;

    private ProductCategory category1;
    private ProductCategory category2;
    private Product product1;
    private Product product2;
    private Product inactiveProduct;
    private Combo combo1;
    private Combo inactiveCombo;
    private ProductOptionGroup optionGroup;
    private ProductOptionValue optionValue1;
    private ProductOptionValue optionValue2;

    @BeforeEach
    void setUp() {
        // 清理資料
        comboItemRepository.deleteAll();
        comboRepository.deleteAll();
        optionValueRepository.deleteAll();
        optionGroupRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // 建立分類
        category1 = categoryRepository.save(ProductCategory.builder()
                .name("漢堡類")
                .isActive(true)
                .sortOrder(1)
                .build());

        category2 = categoryRepository.save(ProductCategory.builder()
                .name("套餐類")
                .isActive(true)
                .sortOrder(2)
                .build());

        // 建立商品
        product1 = productRepository.save(Product.builder()
                .name("招牌漢堡")
                .description("經典美式漢堡")
                .price(new BigDecimal("59.00"))
                .categoryId(category1.getId())
                .categoryName(category1.getName())
                .isActive(true)
                .sortOrder(1)
                .build());

        product2 = productRepository.save(Product.builder()
                .name("紅茶")
                .description("古早味紅茶")
                .price(new BigDecimal("25.00"))
                .categoryId(category1.getId())
                .categoryName(category1.getName())
                .isActive(true)
                .sortOrder(2)
                .build());

        inactiveProduct = productRepository.save(Product.builder()
                .name("停售商品")
                .price(new BigDecimal("99.00"))
                .categoryId(category1.getId())
                .isActive(false)
                .sortOrder(99)
                .build());

        // 建立商品選項
        optionGroup = optionGroupRepository.save(ProductOptionGroup.builder()
                .productId(product1.getId())
                .name("加料")
                .minSelections(0)
                .maxSelections(3)
                .isActive(true)
                .sortOrder(1)
                .build());

        optionValue1 = optionValueRepository.save(ProductOptionValue.builder()
                .groupId(optionGroup.getId())
                .name("加起司")
                .priceAdjustment(new BigDecimal("10.00"))
                .isActive(true)
                .sortOrder(1)
                .build());

        optionValue2 = optionValueRepository.save(ProductOptionValue.builder()
                .groupId(optionGroup.getId())
                .name("加蛋")
                .priceAdjustment(new BigDecimal("15.00"))
                .isActive(true)
                .sortOrder(2)
                .build());

        // 建立套餐
        combo1 = comboRepository.save(Combo.builder()
                .name("超值早餐A")
                .description("漢堡+紅茶")
                .price(new BigDecimal("79.00"))
                .categoryId(category2.getId())
                .categoryName(category2.getName())
                .isActive(true)
                .sortOrder(1)
                .build());

        // 建立套餐項目
        comboItemRepository.save(ComboItem.builder()
                .comboId(combo1.getId())
                .productId(product1.getId())
                .productName(product1.getName())
                .quantity(1)
                .sortOrder(1)
                .build());

        comboItemRepository.save(ComboItem.builder()
                .comboId(combo1.getId())
                .productId(product2.getId())
                .productName(product2.getName())
                .quantity(1)
                .sortOrder(2)
                .build());

        // 建立停用套餐
        inactiveCombo = comboRepository.save(Combo.builder()
                .name("停用套餐")
                .price(new BigDecimal("199.00"))
                .categoryId(category2.getId())
                .isActive(false)
                .sortOrder(99)
                .build());
    }

    // ========== GET /api/pos/menu/list ==========

    @Test
    @DisplayName("GET /api/pos/menu/list - 查詢全部銷售物品成功")
    void listSaleItems_All_Success() throws Exception {
        mockMvc.perform(get("/api/pos/menu/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3)); // 2 商品 + 1 套餐 (不含停用)
    }

    @Test
    @DisplayName("GET /api/pos/menu/list - 依分類查詢成功")
    void listSaleItems_ByCategory_Success() throws Exception {
        mockMvc.perform(get("/api/pos/menu/list")
                        .param("categoryId", category1.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.length()").value(2)) // 只有漢堡類的 2 個商品
                .andExpect(jsonPath("$.data[0].categoryName").value("漢堡類"));
    }

    @Test
    @DisplayName("GET /api/pos/menu/list - 單點商品包含 orderPayload")
    void listSaleItems_SingleProduct_HasOrderPayload() throws Exception {
        mockMvc.perform(get("/api/pos/menu/list")
                        .param("categoryId", category1.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("SINGLE"))
                .andExpect(jsonPath("$.data[0].name").value("招牌漢堡"))
                .andExpect(jsonPath("$.data[0].price").value(59.00))
                .andExpect(jsonPath("$.data[0].orderPayload").isArray())
                .andExpect(jsonPath("$.data[0].orderPayload.length()").value(1))
                .andExpect(jsonPath("$.data[0].orderPayload[0].type").value("SINGLE"))
                .andExpect(jsonPath("$.data[0].orderPayload[0].productId").value(product1.getId()))
                .andExpect(jsonPath("$.data[0].orderPayload[0].productName").value("招牌漢堡"))
                .andExpect(jsonPath("$.data[0].orderPayload[0].unitPrice").value(59.00));
    }

    @Test
    @DisplayName("GET /api/pos/menu/list - 套餐包含 orderPayload（標頭+項目）")
    void listSaleItems_Combo_HasOrderPayload() throws Exception {
        mockMvc.perform(get("/api/pos/menu/list")
                        .param("categoryId", category2.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("COMBO"))
                .andExpect(jsonPath("$.data[0].name").value("超值早餐A"))
                .andExpect(jsonPath("$.data[0].price").value(79.00))
                .andExpect(jsonPath("$.data[0].orderPayload").isArray())
                .andExpect(jsonPath("$.data[0].orderPayload.length()").value(3)) // 1 標頭 + 2 項目
                // 套餐標頭
                .andExpect(jsonPath("$.data[0].orderPayload[0].type").value("COMBO"))
                .andExpect(jsonPath("$.data[0].orderPayload[0].comboId").value(combo1.getId()))
                .andExpect(jsonPath("$.data[0].orderPayload[0].comboName").value("超值早餐A"))
                .andExpect(jsonPath("$.data[0].orderPayload[0].comboPrice").value(79.00))
                // 套餐項目 1
                .andExpect(jsonPath("$.data[0].orderPayload[1].type").value("COMBO_ITEM"))
                .andExpect(jsonPath("$.data[0].orderPayload[1].comboId").value(combo1.getId()))
                .andExpect(jsonPath("$.data[0].orderPayload[1].productId").value(product1.getId()))
                .andExpect(jsonPath("$.data[0].orderPayload[1].productName").value("招牌漢堡"))
                // 套餐項目 2
                .andExpect(jsonPath("$.data[0].orderPayload[2].type").value("COMBO_ITEM"))
                .andExpect(jsonPath("$.data[0].orderPayload[2].productId").value(product2.getId()))
                .andExpect(jsonPath("$.data[0].orderPayload[2].productName").value("紅茶"));
    }

    @Test
    @DisplayName("GET /api/pos/menu/list - 不包含停用商品和套餐")
    void listSaleItems_ExcludesInactive() throws Exception {
        mockMvc.perform(get("/api/pos/menu/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == '停售商品')]").doesNotExist())
                .andExpect(jsonPath("$.data[?(@.name == '停用套餐')]").doesNotExist());
    }

    // ========== GET /api/pos/menu/detail ==========

    @Test
    @DisplayName("GET /api/pos/menu/detail - 查詢單點商品詳情成功（含選項）")
    void getSaleItemDetail_Single_Success() throws Exception {
        mockMvc.perform(get("/api/pos/menu/detail")
                        .param("type", "SINGLE")
                        .param("id", product1.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.type").value("SINGLE"))
                .andExpect(jsonPath("$.data.id").value(product1.getId()))
                .andExpect(jsonPath("$.data.name").value("招牌漢堡"))
                .andExpect(jsonPath("$.data.price").value(59.00))
                // 選項群組
                .andExpect(jsonPath("$.data.optionGroups").isArray())
                .andExpect(jsonPath("$.data.optionGroups.length()").value(1))
                .andExpect(jsonPath("$.data.optionGroups[0].name").value("加料"))
                .andExpect(jsonPath("$.data.optionGroups[0].values").isArray())
                .andExpect(jsonPath("$.data.optionGroups[0].values.length()").value(2))
                .andExpect(jsonPath("$.data.optionGroups[0].values[0].name").value("加起司"))
                .andExpect(jsonPath("$.data.optionGroups[0].values[0].priceAdjustment").value(10.00))
                .andExpect(jsonPath("$.data.optionGroups[0].values[1].name").value("加蛋"))
                .andExpect(jsonPath("$.data.optionGroups[0].values[1].priceAdjustment").value(15.00))
                // orderPayload
                .andExpect(jsonPath("$.data.orderPayload[0].type").value("SINGLE"))
                .andExpect(jsonPath("$.data.orderPayload[0].productId").value(product1.getId()));
    }

    @Test
    @DisplayName("GET /api/pos/menu/detail - 查詢套餐詳情成功（含項目與選項）")
    void getSaleItemDetail_Combo_Success() throws Exception {
        mockMvc.perform(get("/api/pos/menu/detail")
                        .param("type", "COMBO")
                        .param("id", combo1.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.type").value("COMBO"))
                .andExpect(jsonPath("$.data.id").value(combo1.getId()))
                .andExpect(jsonPath("$.data.name").value("超值早餐A"))
                .andExpect(jsonPath("$.data.price").value(79.00))
                // 套餐內商品
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].productName").value("招牌漢堡"))
                .andExpect(jsonPath("$.data.items[0].optionGroups").isArray())
                .andExpect(jsonPath("$.data.items[0].optionGroups[0].name").value("加料"))
                .andExpect(jsonPath("$.data.items[1].productName").value("紅茶"))
                // orderPayload
                .andExpect(jsonPath("$.data.orderPayload.length()").value(3));
    }

    @Test
    @DisplayName("GET /api/pos/menu/detail - 商品不存在 (code=3001)")
    void getSaleItemDetail_ProductNotFound() throws Exception {
        mockMvc.perform(get("/api/pos/menu/detail")
                        .param("type", "SINGLE")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("商品不存在: 99999"));
    }

    @Test
    @DisplayName("GET /api/pos/menu/detail - 套餐不存在 (code=3001)")
    void getSaleItemDetail_ComboNotFound() throws Exception {
        mockMvc.perform(get("/api/pos/menu/detail")
                        .param("type", "COMBO")
                        .param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("套餐不存在: 99999"));
    }

    @Test
    @DisplayName("GET /api/pos/menu/detail - 無效類型 (code=2002)")
    void getSaleItemDetail_InvalidType() throws Exception {
        mockMvc.perform(get("/api/pos/menu/detail")
                        .param("type", "INVALID")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("無效的類型: INVALID"));
    }
}
