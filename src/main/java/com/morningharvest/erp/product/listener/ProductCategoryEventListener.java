package com.morningharvest.erp.product.listener;

import com.morningharvest.erp.product.event.ProductCategoryUpdatedEvent;
import com.morningharvest.erp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品分類事件監聽器
 *
 * 監聽分類相關事件，並同步更新商品中的冗餘資料
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProductCategoryEventListener {

    private final ProductRepository productRepository;

    /**
     * 處理分類更新事件
     *
     * 當分類名稱變更時，批量更新所有相關商品的分類名稱
     */
    @Async
    @EventListener
    @Transactional
    public void onCategoryUpdated(ProductCategoryUpdatedEvent event) {
        log.info("收到分類更新事件: {}", event);

        // 只有名稱變更時才更新商品
        if (event.isNameChanged()) {
            log.info("分類名稱已變更: {} -> {}, 開始更新商品",
                    event.getOldName(), event.getNewName());

            int updatedCount = productRepository.updateCategoryNameByCategoryId(
                    event.getCategoryId(),
                    event.getNewName()
            );

            log.info("已更新 {} 筆商品的分類名稱, categoryId: {}",
                    updatedCount, event.getCategoryId());
        } else {
            log.debug("分類名稱未變更，跳過商品更新");
        }
    }
}
