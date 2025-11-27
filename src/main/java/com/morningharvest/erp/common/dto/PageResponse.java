package com.morningharvest.erp.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 統一的分頁回應格式
 * 精簡版的分頁資料結構，只包含前端必要的欄位
 * 使用 1-based 頁碼（符合前端習慣）
 *
 * @param <T> 分頁資料的類型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /**
     * 資料列表
     */
    private List<T> content;

    /**
     * 當前頁碼（從 1 開始）
     */
    private int page;

    /**
     * 每頁筆數
     */
    private int size;

    /**
     * 總筆數
     */
    private long totalElements;

    /**
     * 總頁數
     */
    private int totalPages;

    /**
     * 是否為第一頁
     */
    private boolean first;

    /**
     * 是否為最後一頁
     */
    private boolean last;

    /**
     * 當前頁實際資料筆數
     */
    private int numberOfElements;

    /**
     * 資料是否為空
     */
    private boolean empty;

    /**
     * 從 Spring Data Page 物件轉換為 PageResponse
     * 自動將 0-based 頁碼轉換為 1-based
     *
     * @param page Spring Data Page 物件
     * @param <T>  資料類型
     * @return PageResponse 物件
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber() + 1)  // 將 0-based 轉換為 1-based
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .numberOfElements(page.getNumberOfElements())
                .empty(page.isEmpty())
                .build();
    }
}
