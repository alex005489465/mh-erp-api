package com.morningharvest.erp.combo.listener;

import com.morningharvest.erp.combo.repository.ComboRepository;
import com.morningharvest.erp.product.event.ProductCategoryUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 套餐分類事件監聯器
 *
 * 監聽分類相關事件，並同步更新套餐中的冗餘資料
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ComboCategoryEventListener {

    private final ComboRepository comboRepository;

    /**
     * 處理分類更新事件
     *
     * 當分類名稱變更時，批量更新所有相關套餐的分類名稱
     */
    @Async
    @EventListener
    @Transactional
    public void onCategoryUpdated(ProductCategoryUpdatedEvent event) {
        log.info("套餐模組收到分類更新事件: {}", event);

        // 只有名稱變更時才更新套餐
        if (event.isNameChanged()) {
            log.info("分類名稱已變更: {} -> {}, 開始更新套餐",
                    event.getOldName(), event.getNewName());

            int updatedCount = comboRepository.updateCategoryNameByCategoryId(
                    event.getCategoryId(),
                    event.getNewName()
            );

            log.info("已更新 {} 筆套餐的分類名稱, categoryId: {}",
                    updatedCount, event.getCategoryId());
        } else {
            log.debug("分類名稱未變更，跳過套餐更新");
        }
    }
}
