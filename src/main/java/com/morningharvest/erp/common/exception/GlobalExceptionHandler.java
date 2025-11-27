package com.morningharvest.erp.common.exception;

import com.morningharvest.erp.common.constant.ResponseCode;
import com.morningharvest.erp.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全域例外處理器
 * 統一處理應用程式中的例外
 *
 * 設計原則:
 * 1. HTTP 層面統一回傳 200 (HTTP 狀態碼由 API Gateway 負責)
 * 2. 業務層面使用 ResponseCode 區分成功/失敗
 * 3. 保留完整的異常日誌記錄
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 處理參數驗證失敗
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("參數驗證失敗: {}", errors);
        return ResponseEntity.ok(
                ApiResponse.error(ResponseCode.VALIDATION_ERROR, "參數驗證失敗", errors)
        );
    }

    /**
     * 處理資源不存在異常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("資源不存在: {}", ex.getMessage());
        return ResponseEntity.ok(
                ApiResponse.error(ResponseCode.RESOURCE_NOT_FOUND, ex.getMessage())
        );
    }

    /**
     * 處理非法參數異常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("非法參數: {}", ex.getMessage());
        return ResponseEntity.ok(
                ApiResponse.error(ResponseCode.ILLEGAL_ARGUMENT, ex.getMessage())
        );
    }

    /**
     * 處理非法狀態異常
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("非法狀態: {}", ex.getMessage());
        return ResponseEntity.ok(
                ApiResponse.error(ResponseCode.ILLEGAL_STATE, ex.getMessage())
        );
    }

    /**
     * 處理檔案大小超過限制異常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {
        log.warn("檔案大小超過限制: {}", ex.getMessage());
        return ResponseEntity.ok(
                ApiResponse.error(ResponseCode.ILLEGAL_ARGUMENT, "檔案大小超過限制，最大允許 10MB")
        );
    }

    /**
     * 處理執行時期例外
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("執行時期錯誤", ex);
        return ResponseEntity.ok(
                ApiResponse.error(ResponseCode.RUNTIME_ERROR, "執行錯誤: " + ex.getMessage())
        );
    }

    /**
     * 處理一般例外
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("系統錯誤", ex);
        return ResponseEntity.ok(
                ApiResponse.error(ResponseCode.SYSTEM_ERROR, "系統發生錯誤: " + ex.getMessage())
        );
    }

}
