package com.morningharvest.erp.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 訂單項目抽象基類
 * 使用 Single Table Inheritance (STI) 策略
 * 子類: SingleOrderItem, ComboOrderItem, ComboItemOrderItem
 */
@Entity
@Table(name = "order_items")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_type", discriminatorType = DiscriminatorType.STRING, length = 15)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "note", length = 200)
    private String note;

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
     * 計算小計 - 由子類實作
     */
    public abstract void calculateSubtotal();

    /**
     * 取得顯示名稱 - 由子類實作
     */
    public abstract String getDisplayName();

    /**
     * 取得項目類型 - 由子類實作
     */
    public abstract String getItemType();
}
