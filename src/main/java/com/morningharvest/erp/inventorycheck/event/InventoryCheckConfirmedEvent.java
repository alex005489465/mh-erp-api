package com.morningharvest.erp.inventorycheck.event;

import com.morningharvest.erp.common.event.BaseEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 盤點確認事件
 *
 * 當盤點單從 IN_PROGRESS 狀態轉為 CONFIRMED 時發布此事件，
 * 用於觸發庫存調整。
 */
@Getter
public class InventoryCheckConfirmedEvent extends BaseEvent {

    private final Long inventoryCheckId;
    private final String checkNumber;
    private final BigDecimal totalDifferenceAmount;
    private final List<InventoryCheckItemInfo> items;

    public InventoryCheckConfirmedEvent(Long inventoryCheckId, String checkNumber,
                                         BigDecimal totalDifferenceAmount,
                                         List<InventoryCheckItemInfo> items) {
        super("INVENTORY_CHECK");
        this.inventoryCheckId = inventoryCheckId;
        this.checkNumber = checkNumber;
        this.totalDifferenceAmount = totalDifferenceAmount;
        this.items = items;
    }

    /**
     * 盤點明細資訊（用於事件傳遞）
     */
    @Getter
    public static class InventoryCheckItemInfo {
        private final Long materialId;
        private final String materialCode;
        private final String materialName;
        private final BigDecimal systemQuantity;
        private final BigDecimal actualQuantity;
        private final BigDecimal differenceQuantity;

        public InventoryCheckItemInfo(Long materialId, String materialCode, String materialName,
                                       BigDecimal systemQuantity, BigDecimal actualQuantity,
                                       BigDecimal differenceQuantity) {
            this.materialId = materialId;
            this.materialCode = materialCode;
            this.materialName = materialName;
            this.systemQuantity = systemQuantity;
            this.actualQuantity = actualQuantity;
            this.differenceQuantity = differenceQuantity;
        }
    }
}
