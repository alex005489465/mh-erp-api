package com.morningharvest.erp.inventorycheck.repository;

import com.morningharvest.erp.inventorycheck.entity.InventoryCheckItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryCheckItemRepository extends JpaRepository<InventoryCheckItem, Long> {

    /**
     * 依盤點單ID查詢明細（按ID排序）
     */
    List<InventoryCheckItem> findByInventoryCheckIdOrderByIdAsc(Long inventoryCheckId);

    /**
     * 依盤點單ID和原物料ID查詢明細
     */
    Optional<InventoryCheckItem> findByInventoryCheckIdAndMaterialId(Long inventoryCheckId, Long materialId);

    /**
     * 刪除盤點單所有明細
     */
    void deleteByInventoryCheckId(Long inventoryCheckId);

    /**
     * 檢查盤點單是否有明細
     */
    boolean existsByInventoryCheckId(Long inventoryCheckId);

    /**
     * 計算未盤點項目數量
     */
    long countByInventoryCheckIdAndIsCheckedFalse(Long inventoryCheckId);

    /**
     * 計算已盤點項目數量
     */
    long countByInventoryCheckIdAndIsCheckedTrue(Long inventoryCheckId);
}
