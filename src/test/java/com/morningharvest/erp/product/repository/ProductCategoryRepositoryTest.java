package com.morningharvest.erp.product.repository;

import com.morningharvest.erp.product.entity.ProductCategory;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ProductCategoryRepository 測試")
class ProductCategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    private ProductCategory activeCategory;
    private ProductCategory inactiveCategory;

    @BeforeEach
    void setUp() {
        // 清除現有資料
        productCategoryRepository.deleteAll();
        entityManager.flush();

        // 建立測試資料
        activeCategory = ProductCategory.builder()
                .name("啟用分類")
                .description("啟用分類說明")
                .isActive(true)
                .sortOrder(1)
                .build();
        entityManager.persistAndFlush(activeCategory);

        inactiveCategory = ProductCategory.builder()
                .name("停用分類")
                .description("停用分類說明")
                .isActive(false)
                .sortOrder(2)
                .build();
        entityManager.persistAndFlush(inactiveCategory);
    }

    @Test
    @DisplayName("existsByName - 名稱存在返回 true")
    void existsByName_WhenExists_ReturnsTrue() {
        // When
        boolean exists = productCategoryRepository.existsByName("啟用分類");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByName - 名稱不存在返回 false")
    void existsByName_WhenNotExists_ReturnsFalse() {
        // When
        boolean exists = productCategoryRepository.existsByName("不存在的分類");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByNameAndIdNot - 其他分類有相同名稱返回 true")
    void existsByNameAndIdNot_WhenExistsAndDifferentId_ReturnsTrue() {
        // When - 檢查 inactiveCategory 的 id 是否有其他分類叫 "啟用分類"
        boolean exists = productCategoryRepository.existsByNameAndIdNot("啟用分類", inactiveCategory.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByNameAndIdNot - 自己的名稱返回 false")
    void existsByNameAndIdNot_WhenSameId_ReturnsFalse() {
        // When - 檢查自己的 id
        boolean exists = productCategoryRepository.existsByNameAndIdNot("啟用分類", activeCategory.getId());

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByIsActive - 篩選啟用分類")
    void findByIsActive_ReturnsOnlyActiveCategories() {
        // When
        Page<ProductCategory> result = productCategoryRepository.findByIsActive(true, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("啟用分類");
        assertThat(result.getContent().get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("findByIsActive - 篩選停用分類")
    void findByIsActive_ReturnsOnlyInactiveCategories() {
        // When
        Page<ProductCategory> result = productCategoryRepository.findByIsActive(false, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("停用分類");
        assertThat(result.getContent().get(0).getIsActive()).isFalse();
    }

    @Test
    @DisplayName("save - 儲存分類並自動設定時間戳")
    void save_SetsTimestamps() {
        // Given
        ProductCategory newCategory = ProductCategory.builder()
                .name("新分類")
                .description("新分類說明")
                .isActive(true)
                .sortOrder(3)
                .build();

        // When
        ProductCategory saved = productCategoryRepository.save(newCategory);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
