package com.morningharvest.erp.pos.dto;

import com.morningharvest.erp.product.dto.ProductOptionGroupDetailDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 銷售物品 DTO
 * 統一表示單點商品或套餐，供 POS 前端使用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleItemDTO {

    /**
     * 類型：SINGLE (單點商品) 或 COMBO (套餐)
     */
    private String type;

    /**
     * ID（商品 ID 或套餐 ID）
     */
    private Long id;

    /**
     * 名稱
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 價格
     */
    private BigDecimal price;

    /**
     * 圖片網址
     */
    private String imageUrl;

    /**
     * 分類 ID
     */
    private Long categoryId;

    /**
     * 分類名稱
     */
    private String categoryName;

    /**
     * 排序順序
     */
    private Integer sortOrder;

    // === 單點商品專用欄位 ===

    /**
     * 選項群組（單點商品用，供客製化選項）
     */
    private List<ProductOptionGroupDetailDTO> optionGroups;

    // === 套餐專用欄位 ===

    /**
     * 套餐內商品列表（套餐用，含各商品的選項群組）
     */
    private List<ComboItemInfoDTO> items;

    // === 訂單 Payload ===

    /**
     * 訂單項目 Payload
     * 前端可直接將此陣列內容加入訂單的 items
     */
    private List<SaleItemPayloadDTO> orderPayload;
}
