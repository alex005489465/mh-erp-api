package com.morningharvest.erp.common.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 統一的分頁請求參數
 * 提供標準的分頁和排序參數
 * 使用 1-based 頁碼（符合前端習慣）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageableRequest {

    /**
     * 頁碼（從 1 開始）
     */
    @Parameter(description = "頁碼（從 1 開始）", example = "1")
    @Min(value = 1, message = "頁碼不能小於 1")
    @Builder.Default
    private int page = 1;

    /**
     * 每頁筆數
     */
    @Parameter(description = "每頁筆數", example = "20")
    @Min(value = 1, message = "每頁筆數不能小於 1")
    @Max(value = 100, message = "每頁筆數不能大於 100")
    @Builder.Default
    private int size = 20;

    /**
     * 排序欄位
     */
    @Parameter(description = "排序欄位", example = "createdAt")
    @Builder.Default
    private String sortBy = "createdAt";

    /**
     * 排序方向
     */
    @Parameter(description = "排序方向（ASC 或 DESC）", example = "DESC")
    @Builder.Default
    private Sort.Direction direction = Sort.Direction.DESC;

    /**
     * 轉換為 Spring Data Pageable 物件
     * 自動將 1-based 頁碼轉換為 0-based
     *
     * @return Pageable 物件
     */
    public Pageable toPageable() {
        return PageRequest.of(page - 1, size, Sort.by(direction, sortBy));
    }

    /**
     * 轉換為 Spring Data Pageable 物件（不排序）
     * 自動將 1-based 頁碼轉換為 0-based
     *
     * @return Pageable 物件
     */
    public Pageable toPageableWithoutSort() {
        return PageRequest.of(page - 1, size);
    }
}
