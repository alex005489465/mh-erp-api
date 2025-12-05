package com.morningharvest.erp.inventorycheck.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_checks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "check_number", length = 30)
    private String checkNumber;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PLANNED";

    @Column(name = "check_date")
    private LocalDate checkDate;

    @Column(name = "total_items")
    @Builder.Default
    private Integer totalItems = 0;

    @Column(name = "total_difference_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalDifferenceAmount = BigDecimal.ZERO;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "started_by", length = 50)
    private String startedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by", length = 50)
    private String confirmedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PLANNED";
        }
        if (totalItems == null) {
            totalItems = 0;
        }
        if (totalDifferenceAmount == null) {
            totalDifferenceAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
