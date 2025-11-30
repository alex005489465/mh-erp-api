package com.morningharvest.erp.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 套餐內商品訂單項目
 * 代表套餐內的一個商品，價格已含在套餐價格中
 * 只計算選項加價
 */
@Entity
@DiscriminatorValue("COMBO_ITEM")
@Getter
@Setter
@NoArgsConstructor
public class ComboItemOrderItem extends OrderItem {

    @Column(name = "combo_id")
    private Long comboId;

    @Column(name = "group_sequence")
    private Integer groupSequence;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", length = 100)
    private String productName;

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "options", columnDefinition = "JSON")
    private String options;

    @Column(name = "options_amount", precision = 10, scale = 2)
    private BigDecimal optionsAmount = BigDecimal.ZERO;

    @Override
    public void calculateSubtotal() {
        // 套餐內商品只計算選項加價，商品價格已含在套餐價格中
        setSubtotal(optionsAmount != null ? optionsAmount : BigDecimal.ZERO);
    }

    @Override
    public String getDisplayName() {
        return productName;
    }

    @Override
    public String getItemType() {
        return "COMBO_ITEM";
    }
}
