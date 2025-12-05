package com.morningharvest.erp.material.listener;

import com.morningharvest.erp.inventorycheck.event.InventoryCheckConfirmedEvent;
import com.morningharvest.erp.material.entity.Material;
import com.morningharvest.erp.material.repository.MaterialRepository;
import com.morningharvest.erp.purchase.event.PurchaseConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 原物料庫存事件監聽器
 *
 * 監聽進貨、盤點相關事件，處理庫存更新
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MaterialStockEventListener {

    private final MaterialRepository materialRepository;

    /**
     * 監聽進貨確認事件
     *
     * 當進貨單確認時，增加對應原物料的庫存數量
     */
    @Async
    @EventListener
    @Transactional
    public void onPurchaseConfirmed(PurchaseConfirmedEvent event) {
        log.info("收到進貨確認事件, purchaseId: {}, purchaseNumber: {}",
                event.getPurchaseId(), event.getPurchaseNumber());

        for (PurchaseConfirmedEvent.PurchaseItemInfo item : event.getItems()) {
            materialRepository.findById(item.getMaterialId())
                    .ifPresentOrElse(
                            material -> updateMaterialStock(material, item),
                            () -> log.warn("原物料不存在, materialId: {}, 跳過庫存更新",
                                    item.getMaterialId())
                    );
        }

        log.info("進貨單庫存更新完成, purchaseId: {}", event.getPurchaseId());
    }

    /**
     * 更新原物料庫存
     */
    private void updateMaterialStock(Material material, PurchaseConfirmedEvent.PurchaseItemInfo item) {
        BigDecimal oldQuantity = material.getCurrentStockQuantity();
        if (oldQuantity == null) {
            oldQuantity = BigDecimal.ZERO;
        }

        BigDecimal newQuantity = oldQuantity.add(item.getQuantity());
        material.setCurrentStockQuantity(newQuantity);

        // 更新成本單價（使用最新進貨價）
        material.setCostPrice(item.getUnitPrice());

        materialRepository.save(material);

        log.info("庫存更新成功, materialId: {}, code: {}, 舊數量: {}, 進貨數量: {}, 新數量: {}",
                material.getId(), material.getCode(),
                oldQuantity, item.getQuantity(), newQuantity);
    }

    /**
     * 監聽盤點確認事件
     *
     * 當盤點單確認時，將庫存調整為實際盤點數量
     */
    @Async
    @EventListener
    @Transactional
    public void onInventoryCheckConfirmed(InventoryCheckConfirmedEvent event) {
        log.info("收到盤點確認事件, inventoryCheckId: {}, checkNumber: {}",
                event.getInventoryCheckId(), event.getCheckNumber());

        for (InventoryCheckConfirmedEvent.InventoryCheckItemInfo item : event.getItems()) {
            materialRepository.findById(item.getMaterialId())
                    .ifPresentOrElse(
                            material -> adjustMaterialStock(material, item),
                            () -> log.warn("原物料不存在, materialId: {}, 跳過庫存調整",
                                    item.getMaterialId())
                    );
        }

        log.info("盤點庫存調整完成, inventoryCheckId: {}", event.getInventoryCheckId());
    }

    /**
     * 調整原物料庫存（盤點）
     */
    private void adjustMaterialStock(Material material,
                                      InventoryCheckConfirmedEvent.InventoryCheckItemInfo item) {
        BigDecimal oldQuantity = material.getCurrentStockQuantity();
        if (oldQuantity == null) {
            oldQuantity = BigDecimal.ZERO;
        }

        // 將庫存設定為實際盤點數量
        material.setCurrentStockQuantity(item.getActualQuantity());
        materialRepository.save(material);

        log.info("庫存調整成功, materialId: {}, code: {}, 舊數量: {}, 實際數量: {}, 盤差: {}",
                material.getId(), material.getCode(),
                oldQuantity, item.getActualQuantity(), item.getDifferenceQuantity());
    }
}
