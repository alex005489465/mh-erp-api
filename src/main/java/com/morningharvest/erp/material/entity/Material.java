package com.morningharvest.erp.material.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "specification", length = 200)
    private String specification;

    @Column(name = "safe_stock_quantity", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal safeStockQuantity = BigDecimal.ZERO;

    @Column(name = "current_stock_quantity", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal currentStockQuantity = BigDecimal.ZERO;

    @Column(name = "cost_price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
}
