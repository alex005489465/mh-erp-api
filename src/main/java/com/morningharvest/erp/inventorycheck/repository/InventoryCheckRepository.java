package com.morningharvest.erp.inventorycheck.repository;

import com.morningharvest.erp.inventorycheck.entity.InventoryCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface InventoryCheckRepository extends JpaRepository<InventoryCheck, Long> {

    /**
     * 檢查盤點單號是否存在
     */
    boolean existsByCheckNumber(String checkNumber);

    /**
     * 依狀態查詢
     */
    Page<InventoryCheck> findByStatus(String status, Pageable pageable);

    /**
     * 複合查詢：關鍵字 + 狀態 + 日期範圍
     */
    @Query("SELECT ic FROM InventoryCheck ic WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR ic.checkNumber LIKE %:keyword%) " +
           "AND (:status IS NULL OR ic.status = :status) " +
           "AND (:startDate IS NULL OR ic.checkDate >= :startDate) " +
           "AND (:endDate IS NULL OR ic.checkDate <= :endDate)")
    Page<InventoryCheck> findByFilters(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * 計算指定前綴的盤點單數量（用於產生單號）
     */
    @Query("SELECT COUNT(ic) FROM InventoryCheck ic WHERE ic.checkNumber LIKE :prefix%")
    long countByCheckNumberPrefix(@Param("prefix") String prefix);
}
