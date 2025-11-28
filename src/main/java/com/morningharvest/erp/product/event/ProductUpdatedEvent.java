package com.morningharvest.erp.product.event;

import com.morningharvest.erp.common.event.BaseEvent;
import com.morningharvest.erp.product.dto.ProductDTO;
import lombok.Getter;

import java.util.Objects;

/**
 * 商品更新事件
 *
 * 當商品被更新時發布此事件，包含更新前後的完整資料
 */
@Getter
public class ProductUpdatedEvent extends BaseEvent {

    /**
     * 更新前的商品資料
     */
    private final ProductDTO before;

    /**
     * 更新後的商品資料
     */
    private final ProductDTO after;

    public ProductUpdatedEvent(ProductDTO before, ProductDTO after) {
        super("PRODUCT");
        this.before = before;
        this.after = after;
    }

    /**
     * 取得商品 ID
     */
    public Long getProductId() {
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
        return String.format("%s[eventId=%s, productId=%d, nameChanged=%s, oldName=%s, newName=%s]",
                getEventType(), getEventId(), getProductId(), isNameChanged(), getOldName(), getNewName());
    }
}
