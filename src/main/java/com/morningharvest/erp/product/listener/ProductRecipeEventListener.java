package com.morningharvest.erp.product.listener;

import com.morningharvest.erp.material.event.MaterialUpdatedEvent;
import com.morningharvest.erp.product.event.ProductUpdatedEvent;
import com.morningharvest.erp.product.repository.ProductRecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品配方事件監聽器
 *
 * 監聽商品和原物料更新事件，同步更新配方中的冗餘欄位
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProductRecipeEventListener {

    private final ProductRecipeRepository productRecipeRepository;

    /**
     * 處理商品更新事件
     *
     * 當商品名稱變更時，更新所有配方中的商品名稱
     */
    @Async
    @EventListener
    @Transactional
    public void onProductUpdated(ProductUpdatedEvent event) {
        log.info("收到商品更新事件: {}", event);

        if (event.isNameChanged()) {
            log.info("商品名稱已變更: {} -> {}, 開始更新配方",
                    event.getOldName(), event.getNewName());

            productRecipeRepository.updateProductNameByProductId(
                    event.getProductId(),
                    event.getNewName()
            );

            log.info("已更新商品配方中的商品名稱, productId: {}", event.getProductId());
        }
    }

    /**
     * 處理原物料更新事件
     *
     * 當原物料編號、名稱或單位變更時，更新所有配方中的對應欄位
     */
    @Async
    @EventListener
    @Transactional
    public void onMaterialUpdated(MaterialUpdatedEvent event) {
        log.info("收到原物料更新事件: {}", event);

        if (event.isCodeChanged() || event.isNameChanged() || event.isUnitChanged()) {
            log.info("原物料資訊已變更, 開始更新配方, materialId: {}", event.getMaterialId());

            productRecipeRepository.updateMaterialInfoByMaterialId(
                    event.getMaterialId(),
                    event.getNewCode(),
                    event.getNewName(),
                    event.getNewUnit()
            );

            log.info("已更新配方中的原物料資訊, materialId: {}", event.getMaterialId());
        }
    }
}
