package com.morningharvest.erp.supplier.repository;

import com.morningharvest.erp.supplier.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 供應商資料存取層
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    /**
     * 檢查供應商編號是否存在
     */
    boolean existsByCode(String code);

    /**
     * 檢查供應商編號是否存在（排除指定 ID）
     */
    boolean existsByCodeAndIdNot(String code, Long id);

    /**
     * 依啟用狀態查詢供應商列表
     */
    Page<Supplier> findByIsActive(Boolean isActive, Pageable pageable);

    /**
     * 依關鍵字搜尋供應商（搜尋編號、名稱、簡稱、聯絡人）
     */
    @Query("SELECT s FROM Supplier s WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "s.code LIKE %:keyword% OR " +
           "s.name LIKE %:keyword% OR " +
           "s.shortName LIKE %:keyword% OR " +
           "s.contactPerson LIKE %:keyword%)")
    Page<Supplier> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 依關鍵字和啟用狀態搜尋供應商
     */
    @Query("SELECT s FROM Supplier s WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "s.code LIKE %:keyword% OR " +
           "s.name LIKE %:keyword% OR " +
           "s.shortName LIKE %:keyword% OR " +
           "s.contactPerson LIKE %:keyword%) " +
           "AND (:isActive IS NULL OR s.isActive = :isActive)")
    Page<Supplier> findByKeywordAndIsActive(
            @Param("keyword") String keyword,
            @Param("isActive") Boolean isActive,
            Pageable pageable);
}
