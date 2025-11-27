package com.morningharvest.erp.product.event;

import com.morningharvest.erp.common.event.BaseEvent;
import com.morningharvest.erp.product.dto.ProductCategoryDTO;
import lombok.Getter;

import java.util.Objects;

/**
 * 商品分類更新事件
 *
 * 當商品分類被更新時發布此事件，包含更新前後的完整資料
 */
@Getter
public class ProductCategoryUpdatedEvent extends BaseEvent {

    /**
     * 更新前的分類資料
     */
    private final ProductCategoryDTO before;

    /**
     * 更新後的分類資料
     */
    private final ProductCategoryDTO after;

    public ProductCategoryUpdatedEvent(ProductCategoryDTO before, ProductCategoryDTO after) {
        super("PRODUCT_CATEGORY");
        this.before = before;
        this.after = after;
    }

    /**
     * 取得分類 ID
     */
    public Long getCategoryId() {
        return after.getId();
    }

    /**
     * 檢查名稱是否有變更
     */
    public boolean isNameChanged() {
        return !Objects.equals(before.getName(), after.getName());
    }

    /**
     * 取得舊名稱
     */
    public String getOldName() {
        return before.getName();
    }

    /**
     * 取得新名稱
     */
    public String getNewName() {
        return after.getName();
    }

    @Override
    public String toString() {
        return String.format("%s[eventId=%s, categoryId=%d, nameChanged=%s, oldName=%s, newName=%s]",
                getEventType(), getEventId(), getCategoryId(), isNameChanged(), getOldName(), getNewName());
    }
}
