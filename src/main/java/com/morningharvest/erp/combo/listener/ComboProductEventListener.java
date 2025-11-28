package com.morningharvest.erp.combo.listener;

import com.morningharvest.erp.combo.repository.ComboItemRepository;
import com.morningharvest.erp.product.event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 套餐商品事件監聽器
 *
 * 監聽商品相關事件，並同步更新套餐項目中的冗餘資料
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ComboProductEventListener {

    private final ComboItemRepository comboItemRepository;

    /**
     * 處理商品更新事件
     *
     * 當商品名稱變更時，批量更新所有相關套餐項目的商品名稱
     */
    @Async
    @EventListener
    @Transactional
    public void onProductUpdated(ProductUpdatedEvent event) {
        log.info("套餐模組收到商品更新事件: {}", event);

        // 只有名稱變更時才更新套餐項目
        if (event.isNameChanged()) {
            log.info("商品名稱已變更: {} -> {}, 開始更新套餐項目",
                    event.getOldName(), event.getNewName());

            int updatedCount = comboItemRepository.updateProductNameByProductId(
                    event.getProductId(),
                    event.getNewName()
            );

            log.info("已更新 {} 筆套餐項目的商品名稱, productId: {}",
                    updatedCount, event.getProductId());
        } else {
            log.debug("商品名稱未變更，跳過套餐項目更新");
        }
    }
}
