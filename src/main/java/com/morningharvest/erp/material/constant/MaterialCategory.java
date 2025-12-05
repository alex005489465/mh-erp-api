package com.morningharvest.erp.material.constant;

/**
 * 原物料分類常數
 */
public final class MaterialCategory {

    public static final String BREAD = "BREAD";           // 麵包類
    public static final String EGG = "EGG";               // 蛋類
    public static final String MEAT = "MEAT";             // 肉類
    public static final String BEVERAGE = "BEVERAGE";     // 飲料類
    public static final String SEASONING = "SEASONING";   // 調味料
    public static final String DAIRY = "DAIRY";           // 乳製品
    public static final String VEGETABLE = "VEGETABLE";   // 蔬菜類
    public static final String FRUIT = "FRUIT";           // 水果類
    public static final String OTHER = "OTHER";           // 其他

    /**
     * 所有有效的分類值
     */
    public static final String[] ALL_CATEGORIES = {
            BREAD, EGG, MEAT, BEVERAGE, SEASONING, DAIRY, VEGETABLE, FRUIT, OTHER
    };

    /**
     * 取得分類的中文顯示名稱
     */
    public static String getDisplayName(String category) {
        if (category == null) {
            return null;
        }
        return switch (category) {
            case BREAD -> "麵包類";
            case EGG -> "蛋類";
            case MEAT -> "肉類";
            case BEVERAGE -> "飲料類";
            case SEASONING -> "調味料";
            case DAIRY -> "乳製品";
            case VEGETABLE -> "蔬菜類";
            case FRUIT -> "水果類";
            case OTHER -> "其他";
            default -> category;
        };
    }

    /**
     * 檢查是否為有效的分類值
     */
    public static boolean isValid(String category) {
        if (category == null) {
            return false;
        }
        for (String validCategory : ALL_CATEGORIES) {
            if (validCategory.equals(category)) {
                return true;
            }
        }
        return false;
    }

    private MaterialCategory() {
    }
}
