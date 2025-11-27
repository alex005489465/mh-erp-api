package com.morningharvest.erp.product.repository;

import com.morningharvest.erp.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ProductRepository 測試")
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private Product activeProduct;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        // 清除現有資料
        productRepository.deleteAll();
        entityManager.flush();

        // 建立測試資料
        activeProduct = Product.builder()
                .name("上架商品")
                .description("上架商品說明")
                .price(new BigDecimal("50.00"))
                .isActive(true)
                .sortOrder(1)
                .build();
        entityManager.persistAndFlush(activeProduct);

        inactiveProduct = Product.builder()
                .name("下架商品")
                .description("下架商品說明")
                .price(new BigDecimal("60.00"))
                .isActive(false)
                .sortOrder(2)
                .build();
        entityManager.persistAndFlush(inactiveProduct);
    }

    @Test
    @DisplayName("existsByName - 名稱存在返回 true")
    void existsByName_WhenExists_ReturnsTrue() {
        // When
        boolean exists = productRepository.existsByName("上架商品");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByName - 名稱不存在返回 false")
    void existsByName_WhenNotExists_ReturnsFalse() {
        // When
        boolean exists = productRepository.existsByName("不存在的商品");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByNameAndIdNot - 其他商品有相同名稱返回 true")
    void existsByNameAndIdNot_WhenExistsAndDifferentId_ReturnsTrue() {
        // When - 檢查 inactiveProduct 的 id 是否有其他商品叫 "上架商品"
        boolean exists = productRepository.existsByNameAndIdNot("上架商品", inactiveProduct.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByNameAndIdNot - 自己的名稱返回 false")
    void existsByNameAndIdNot_WhenSameId_ReturnsFalse() {
        // When - 檢查自己的 id
        boolean exists = productRepository.existsByNameAndIdNot("上架商品", activeProduct.getId());

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByIsActive - 篩選上架商品")
    void findByIsActive_ReturnsOnlyActiveProducts() {
        // When
        Page<Product> result = productRepository.findByIsActive(true, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("上架商品");
        assertThat(result.getContent().get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("findByIsActive - 篩選下架商品")
    void findByIsActive_ReturnsOnlyInactiveProducts() {
        // When
        Page<Product> result = productRepository.findByIsActive(false, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("下架商品");
        assertThat(result.getContent().get(0).getIsActive()).isFalse();
    }

    @Test
    @DisplayName("save - 儲存商品並自動設定時間戳")
    void save_SetsTimestamps() {
        // Given
        Product newProduct = Product.builder()
                .name("新商品")
                .price(new BigDecimal("70.00"))
                .isActive(true)
                .sortOrder(3)
                .build();

        // When
        Product saved = productRepository.save(newProduct);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
