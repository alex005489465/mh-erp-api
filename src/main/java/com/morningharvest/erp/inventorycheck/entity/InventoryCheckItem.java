package com.morningharvest.erp.inventorycheck.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_check_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCheckItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_check_id")
    private Long inventoryCheckId;

    @Column(name = "material_id")
    private Long materialId;

    @Column(name = "material_code", length = 20)
    private String materialCode;

    @Column(name = "material_name", length = 100)
    private String materialName;

    @Column(name = "material_unit", length = 20)
    private String materialUnit;

    @Column(name = "system_quantity", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal systemQuantity = BigDecimal.ZERO;

    @Column(name = "actual_quantity", precision = 10, scale = 2)
    private BigDecimal actualQuantity;

    @Column(name = "difference_quantity", precision = 10, scale = 2)
    private BigDecimal differenceQuantity;

    @Column(name = "unit_cost", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "difference_amount", precision = 12, scale = 2)
    private BigDecimal differenceAmount;

    @Column(name = "is_checked")
    @Builder.Default
    private Boolean isChecked = false;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isChecked == null) {
            isChecked = false;
        }
        if (systemQuantity == null) {
            systemQuantity = BigDecimal.ZERO;
        }
        if (unitCost == null) {
            unitCost = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 計算盤差數量和金額
     */
    public void calculateDifference() {
        if (actualQuantity != null && systemQuantity != null) {
            differenceQuantity = actualQuantity.subtract(systemQuantity);
            if (unitCost != null) {
                differenceAmount = differenceQuantity.multiply(unitCost);
            } else {
                differenceAmount = BigDecimal.ZERO;
            }
        }
    }
}
