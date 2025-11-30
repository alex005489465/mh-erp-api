package com.morningharvest.erp.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "options", columnDefinition = "JSON")
    private String options;

    @Column(name = "options_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal optionsAmount = BigDecimal.ZERO;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "item_type", nullable = false, length = 10)
    @Builder.Default
    private String itemType = "SINGLE";

    @Column(name = "combo_id")
    private Long comboId;

    @Column(name = "combo_name", length = 100)
    private String comboName;

    @Column(name = "group_sequence")
    private Integer groupSequence;

    @Column(name = "combo_price", precision = 10, scale = 2)
    private BigDecimal comboPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 計算小計
     * - 單點項目: (unit_price + options_amount) * quantity
     * - 套餐項目: combo_price (僅主項目有值) + options_amount
     */
    public void calculateSubtotal() {
        if ("COMBO".equals(this.itemType)) {
            BigDecimal basePrice = (this.comboPrice != null) ? this.comboPrice : BigDecimal.ZERO;
            this.subtotal = basePrice.add(this.optionsAmount);
        } else {
            this.subtotal = this.unitPrice
                    .add(this.optionsAmount)
                    .multiply(BigDecimal.valueOf(this.quantity));
        }
    }
}
