package com.morningharvest.erp.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 套餐標題訂單項目
 * 代表一個套餐的整體記錄，包含套餐價格
 */
@Entity
@DiscriminatorValue("COMBO")
@Getter
@Setter
@NoArgsConstructor
public class ComboOrderItem extends OrderItem {

    @Column(name = "combo_id")
    private Long comboId;

    @Column(name = "combo_name", length = 100)
    private String comboName;

    @Column(name = "combo_price", precision = 10, scale = 2)
    private BigDecimal comboPrice;

    @Column(name = "group_sequence")
    private Integer groupSequence;

    @Override
    public void calculateSubtotal() {
        setSubtotal(comboPrice != null ? comboPrice : BigDecimal.ZERO);
    }

    @Override
    public String getDisplayName() {
        return comboName;
    }

    @Override
    public String getItemType() {
        return "COMBO";
    }
}
