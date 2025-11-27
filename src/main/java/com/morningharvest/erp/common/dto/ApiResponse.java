package com.morningharvest.erp.common.dto;

import com.morningharvest.erp.common.constant.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 統一 API 回應格式
 *
 * @param <T> 回應資料的類型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 業務狀態碼
     */
    private Integer code;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 回應訊息
     */
    private String message;

    /**
     * 回應資料
     */
    private T data;

    /**
     * 時間戳記
     */
    private LocalDateTime timestamp;

    /**
     * 成功回應 (帶資料)
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                ResponseCode.SUCCESS.getCode(),
                true,
                ResponseCode.SUCCESS.getMessage(),
                data,
                LocalDateTime.now()
        );
    }

    /**
     * 成功回應 (帶訊息和資料)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                ResponseCode.SUCCESS.getCode(),
                true,
                message,
                data,
                LocalDateTime.now()
        );
    }

    /**
     * 成功回應 (只有訊息)
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(
                ResponseCode.SUCCESS.getCode(),
                true,
                message,
                null,
                LocalDateTime.now()
        );
    }

    /**
     * 失敗回應 (使用 ResponseCode)
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode) {
        return new ApiResponse<>(
                responseCode.getCode(),
                false,
                responseCode.getMessage(),
                null,
                LocalDateTime.now()
        );
    }

    /**
     * 失敗回應 (使用 ResponseCode + 自訂訊息)
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode, String message) {
        return new ApiResponse<>(
                responseCode.getCode(),
                false,
                message,
                null,
                LocalDateTime.now()
        );
    }

    /**
     * 失敗回應 (使用 ResponseCode + 自訂訊息 + 資料)
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode, String message, T data) {
        return new ApiResponse<>(
                responseCode.getCode(),
                false,
                message,
                data,
                LocalDateTime.now()
        );
    }

}
