package com.morningharvest.erp.purchase.repository;

import com.morningharvest.erp.purchase.entity.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    /**
     * 檢查進貨單號是否存在
     */
    boolean existsByPurchaseNumber(String purchaseNumber);

    /**
     * 依狀態查詢
     */
    Page<Purchase> findByStatus(String status, Pageable pageable);

    /**
     * 依供應商ID查詢
     */
    Page<Purchase> findBySupplierId(Long supplierId, Pageable pageable);

    /**
     * 依進貨日期範圍查詢
     */
    Page<Purchase> findByPurchaseDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * 組合查詢：關鍵字 + 狀態 + 供應商 + 日期範圍
     */
    @Query("SELECT p FROM Purchase p WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "p.purchaseNumber LIKE %:keyword% OR " +
           "p.supplierName LIKE %:keyword%) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:supplierId IS NULL OR p.supplierId = :supplierId) " +
           "AND (:startDate IS NULL OR p.purchaseDate >= :startDate) " +
           "AND (:endDate IS NULL OR p.purchaseDate <= :endDate)")
    Page<Purchase> findByFilters(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("supplierId") Long supplierId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * 統計指定前綴的進貨單數量（用於產生單號）
     */
    @Query("SELECT COUNT(p) FROM Purchase p WHERE p.purchaseNumber LIKE :prefix%")
    long countByPurchaseNumberPrefix(@Param("prefix") String prefix);
}
