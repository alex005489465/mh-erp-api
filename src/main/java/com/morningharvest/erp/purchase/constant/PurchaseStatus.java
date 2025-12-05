package com.morningharvest.erp.purchase.constant;

import java.util.Map;

/**
 * 進貨單狀態常數
 */
public final class PurchaseStatus {

    private PurchaseStatus() {}

    /**
     * 草稿
     */
    public static final String DRAFT = "DRAFT";

    /**
     * 已確認
     */
    public static final String CONFIRMED = "CONFIRMED";

    /**
     * 所有狀態
     */
    public static final String[] ALL_STATUSES = {DRAFT, CONFIRMED};

    /**
     * 狀態顯示名稱對應
     */
    private static final Map<String, String> DISPLAY_NAMES = Map.of(
            DRAFT, "草稿",
            CONFIRMED, "已確認"
    );

    /**
     * 取得狀態顯示名稱
     */
    public static String getDisplayName(String status) {
        return DISPLAY_NAMES.getOrDefault(status, status);
    }

    /**
     * 驗證狀態是否有效
     */
    public static boolean isValid(String status) {
        for (String s : ALL_STATUSES) {
            if (s.equals(status)) {
                return true;
            }
        }
        return false;
    }
}
