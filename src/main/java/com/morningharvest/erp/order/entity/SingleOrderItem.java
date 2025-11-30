package com.morningharvest.erp.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 單點商品訂單項目
 */
@Entity
@DiscriminatorValue("SINGLE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SingleOrderItem extends OrderItem {

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", length = 100)
    private String productName;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "options", columnDefinition = "JSON")
    private String options;

    @Column(name = "options_amount", precision = 10, scale = 2)
    private BigDecimal optionsAmount = BigDecimal.ZERO;

    @Override
    public void calculateSubtotal() {
        BigDecimal base = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        BigDecimal optAmt = optionsAmount != null ? optionsAmount : BigDecimal.ZERO;
        int qty = quantity != null ? quantity : 1;
        setSubtotal(base.add(optAmt).multiply(BigDecimal.valueOf(qty)));
    }

    @Override
    public String getDisplayName() {
        return productName;
    }

    @Override
    public String getItemType() {
        return "SINGLE";
    }
}
