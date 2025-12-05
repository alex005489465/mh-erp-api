package com.morningharvest.erp.supplier.constant;

/**
 * 付款條件常數
 */
public final class PaymentTerms {

    /**
     * 貨到付款 (Cash on Delivery)
     */
    public static final String COD = "COD";

    /**
     * 30 天付款
     */
    public static final String NET30 = "NET30";

    /**
     * 60 天付款
     */
    public static final String NET60 = "NET60";

    /**
     * 90 天付款
     */
    public static final String NET90 = "NET90";

    /**
     * 所有付款條件
     */
    public static final String[] ALL_TERMS = {
            COD, NET30, NET60, NET90
    };

    private PaymentTerms() {
        // 防止實例化
    }

    /**
     * 取得付款條件的中文顯示名稱
     */
    public static String getDisplayName(String term) {
        if (term == null) {
            return null;
        }
        return switch (term) {
            case COD -> "貨到付款";
            case NET30 -> "30 天付款";
            case NET60 -> "60 天付款";
            case NET90 -> "90 天付款";
            default -> term;
        };
    }

    /**
     * 檢查付款條件是否有效
     */
    public static boolean isValid(String term) {
        if (term == null) {
            return false;
        }
        for (String validTerm : ALL_TERMS) {
            if (validTerm.equals(term)) {
                return true;
            }
        }
        return false;
    }
}
