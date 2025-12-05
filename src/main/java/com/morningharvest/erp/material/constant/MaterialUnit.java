package com.morningharvest.erp.material.constant;

/**
 * 原物料單位常數
 */
public final class MaterialUnit {

    public static final String PIECE = "PIECE";         // 個
    public static final String PACK = "PACK";           // 包
    public static final String KILOGRAM = "KILOGRAM";   // 公斤
    public static final String LITER = "LITER";         // 公升
    public static final String DOZEN = "DOZEN";         // 打
    public static final String BOX = "BOX";             // 盒
    public static final String STRIP = "STRIP";         // 條
    public static final String SLICE = "SLICE";         // 片

    /**
     * 所有有效的單位值
     */
    public static final String[] ALL_UNITS = {
            PIECE, PACK, KILOGRAM, LITER, DOZEN, BOX, STRIP, SLICE
    };

    /**
     * 取得單位的中文顯示名稱
     */
    public static String getDisplayName(String unit) {
        if (unit == null) {
            return null;
        }
        return switch (unit) {
            case PIECE -> "個";
            case PACK -> "包";
            case KILOGRAM -> "公斤";
            case LITER -> "公升";
            case DOZEN -> "打";
            case BOX -> "盒";
            case STRIP -> "條";
            case SLICE -> "片";
            default -> unit;
        };
    }

    /**
     * 檢查是否為有效的單位值
     */
    public static boolean isValid(String unit) {
        if (unit == null) {
            return false;
        }
        for (String validUnit : ALL_UNITS) {
            if (validUnit.equals(unit)) {
                return true;
            }
        }
        return false;
    }

    private MaterialUnit() {
    }
}
