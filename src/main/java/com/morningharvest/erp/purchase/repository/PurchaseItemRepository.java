package com.morningharvest.erp.purchase.repository;

import com.morningharvest.erp.purchase.entity.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {

    /**
     * 依進貨單ID查詢明細
     */
    List<PurchaseItem> findByPurchaseIdOrderByIdAsc(Long purchaseId);

    /**
     * 刪除進貨單的所有明細
     */
    void deleteByPurchaseId(Long purchaseId);

    /**
     * 檢查進貨單是否有明細
     */
    boolean existsByPurchaseId(Long purchaseId);
}
