package com.morningharvest.erp.common.constant;

import lombok.Getter;

/**
 * 業務狀態碼
 * 統一使用數字碼,從 1000 開始
 */
@Getter
public enum ResponseCode {

    /**
     * 成功
     */
    SUCCESS(1000, "操作成功"),

    /**
     * 參數驗證錯誤 (2xxx)
     */
    VALIDATION_ERROR(2001, "參數驗證失敗"),
    ILLEGAL_ARGUMENT(2002, "非法參數"),
    ILLEGAL_STATE(2003, "非法狀態"),

    /**
     * 資源錯誤 (3xxx)
     */
    RESOURCE_NOT_FOUND(3001, "資源不存在"),

    /**
     * 系統錯誤 (5xxx)
     */
    SYSTEM_ERROR(5000, "系統錯誤"),
    RUNTIME_ERROR(5001, "執行錯誤");

    /**
     * 狀態碼
     */
    private final Integer code;

    /**
     * 預設訊息
     */
    private final String message;

    ResponseCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
