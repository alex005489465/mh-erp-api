package com.morningharvest.erp.inventorycheck.constant;

import java.util.Map;

/**
 * 庫存盤點狀態常數
 */
public final class InventoryCheckStatus {

    private InventoryCheckStatus() {}

    /**
     * 計畫中 - 建立盤點計畫後的初始狀態
     */
    public static final String PLANNED = "PLANNED";

    /**
     * 盤點中 - 開始盤點後的狀態
     */
    public static final String IN_PROGRESS = "IN_PROGRESS";

    /**
     * 已確認 - 盤點完成並確認，庫存已調整
     */
    public static final String CONFIRMED = "CONFIRMED";

    /**
     * 所有狀態
     */
    public static final String[] ALL_STATUSES = {PLANNED, IN_PROGRESS, CONFIRMED};

    /**
     * 狀態顯示名稱對應
     */
    private static final Map<String, String> DISPLAY_NAMES = Map.of(
            PLANNED, "計畫中",
            IN_PROGRESS, "盤點中",
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
