package com.morningharvest.erp.supplier.repository;

import com.morningharvest.erp.common.test.TestDataFactory;
import com.morningharvest.erp.supplier.entity.Supplier;
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

/**
 * SupplierRepository 測試
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("SupplierRepository 測試")
class SupplierRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SupplierRepository supplierRepository;

    private Supplier activeSupplier;
    private Supplier inactiveSupplier;

    @BeforeEach
    void setUp() {
        // 清除現有資料
        supplierRepository.deleteAll();
        entityManager.flush();

        // 建立測試資料
        activeSupplier = TestDataFactory.defaultSupplier().build();
        entityManager.persistAndFlush(activeSupplier);

        inactiveSupplier = TestDataFactory.inactiveSupplier().build();
        entityManager.persistAndFlush(inactiveSupplier);
    }

    @Test
    @DisplayName("existsByCode - 編號存在返回 true")
    void existsByCode_WhenExists_ReturnsTrue() {
        // When
        boolean exists = supplierRepository.existsByCode("S001");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByCode - 編號不存在返回 false")
    void existsByCode_WhenNotExists_ReturnsFalse() {
        // When
        boolean exists = supplierRepository.existsByCode("S999");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByCodeAndIdNot - 排除自己的唯一性檢查")
    void existsByCodeAndIdNot_Success() {
        // When - 查詢自己的編號，排除自己
        boolean existsExcludingSelf = supplierRepository.existsByCodeAndIdNot("S001", activeSupplier.getId());
        // When - 查詢別人的編號
        boolean existsOtherCode = supplierRepository.existsByCodeAndIdNot("S002", activeSupplier.getId());

        // Then
        assertThat(existsExcludingSelf).isFalse();  // 排除自己後不存在
        assertThat(existsOtherCode).isTrue();        // 其他編號存在
    }

    @Test
    @DisplayName("findByIsActive - 篩選啟用供應商")
    void findByIsActive_ReturnsOnlyActiveSuppliers() {
        // When
        Page<Supplier> result = supplierRepository.findByIsActive(true, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCode()).isEqualTo("S001");
        assertThat(result.getContent().get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("findByKeywordAndIsActive - 關鍵字 + 狀態篩選")
    void findByKeywordAndIsActive_Success() {
        // When - 關鍵字搜尋（名稱）
        Page<Supplier> resultByName = supplierRepository.findByKeywordAndIsActive(
                "測試", null, PageRequest.of(0, 10));

        // When - 關鍵字 + 啟用狀態
        Page<Supplier> resultActiveOnly = supplierRepository.findByKeywordAndIsActive(
                null, true, PageRequest.of(0, 10));

        // When - 關鍵字搜尋（聯絡人）
        Page<Supplier> resultByContact = supplierRepository.findByKeywordAndIsActive(
                "王小明", null, PageRequest.of(0, 10));

        // Then
        assertThat(resultByName.getContent()).hasSize(1);
        assertThat(resultByName.getContent().get(0).getName()).contains("測試");

        assertThat(resultActiveOnly.getContent()).hasSize(1);
        assertThat(resultActiveOnly.getContent().get(0).getIsActive()).isTrue();

        assertThat(resultByContact.getContent()).hasSize(1);
        assertThat(resultByContact.getContent().get(0).getContactPerson()).isEqualTo("王小明");
    }

    @Test
    @DisplayName("save - 儲存供應商並自動設定時間戳")
    void save_SetsTimestamps() {
        // Given
        Supplier newSupplier = Supplier.builder()
                .code("S003")
                .name("新供應商")
                .isActive(true)
                .build();

        // When
        Supplier saved = supplierRepository.save(newSupplier);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
