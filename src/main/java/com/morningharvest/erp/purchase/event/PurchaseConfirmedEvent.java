package com.morningharvest.erp.purchase.event;

import com.morningharvest.erp.common.event.BaseEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 進貨確認事件
 *
 * 當進貨單從 DRAFT 變更為 CONFIRMED 時發布
 * 用於觸發庫存更新
 */
@Getter
public class PurchaseConfirmedEvent extends BaseEvent {

    private final Long purchaseId;
    private final String purchaseNumber;
    private final BigDecimal totalAmount;
    private final List<PurchaseItemInfo> items;

    public PurchaseConfirmedEvent(Long purchaseId, String purchaseNumber,
                                   BigDecimal totalAmount, List<PurchaseItemInfo> items) {
        super("PURCHASE");
        this.purchaseId = purchaseId;
        this.purchaseNumber = purchaseNumber;
        this.totalAmount = totalAmount;
        this.items = items;
    }

    /**
     * 進貨明細資訊（用於事件傳遞）
     */
    @Getter
    public static class PurchaseItemInfo {
        private final Long materialId;
        private final String materialCode;
        private final String materialName;
        private final BigDecimal quantity;
        private final BigDecimal unitPrice;

        public PurchaseItemInfo(Long materialId, String materialCode,
                                String materialName, BigDecimal quantity, BigDecimal unitPrice) {
            this.materialId = materialId;
            this.materialCode = materialCode;
            this.materialName = materialName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
    }
}
