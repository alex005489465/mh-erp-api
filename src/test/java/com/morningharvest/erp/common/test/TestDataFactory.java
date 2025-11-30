package com.morningharvest.erp.common.test;

import com.morningharvest.erp.order.dto.OrderItemOptionDTO;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.entity.SingleOrderItem;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.entity.ProductCategory;
import com.morningharvest.erp.product.entity.ProductOptionGroup;
import com.morningharvest.erp.product.entity.ProductOptionValue;

import java.math.BigDecimal;

/**
 * 測試資料工廠類別
 * 提供訂單相關 Entity 的預設 Builder 方法，簡化測試資料建立
 */
public final class TestDataFactory {

    private TestDataFactory() {
        // 防止實例化
    }

    // ===== ProductCategory =====

    /**
     * 建立預設的產品分類 Builder
     * 預設值: name="測試分類", isActive=true, sortOrder=1
     */
    public static ProductCategory.ProductCategoryBuilder defaultCategory() {
        return ProductCategory.builder()
                .name("測試分類")
                .isActive(true)
                .sortOrder(1);
    }

    // ===== Product =====

    /**
     * 建立預設的產品 Builder
     * 預設值: name="測試商品", price=59.00, isActive=true, sortOrder=1
     */
    public static Product.ProductBuilder defaultProduct() {
        return Product.builder()
                .name("測試商品")
                .price(new BigDecimal("59.00"))
                .isActive(true)
                .sortOrder(1);
    }

    /**
     * 建立停售的產品 Builder
     */
    public static Product.ProductBuilder inactiveProduct() {
        return Product.builder()
                .name("停售商品")
                .price(new BigDecimal("49.00"))
                .isActive(false)
                .sortOrder(99);
    }

    // ===== ProductOptionGroup =====

    /**
     * 建立預設的產品選項群組 Builder
     * 預設值: name="加料", minSelections=0, maxSelections=3, isActive=true, sortOrder=1
     */
    public static ProductOptionGroup.ProductOptionGroupBuilder defaultOptionGroup() {
        return ProductOptionGroup.builder()
                .name("加料")
                .minSelections(0)
                .maxSelections(3)
                .isActive(true)
                .sortOrder(1);
    }

    /**
     * 建立必選的產品選項群組 Builder (至少選 1 個)
     */
    public static ProductOptionGroup.ProductOptionGroupBuilder requiredOptionGroup() {
        return ProductOptionGroup.builder()
                .name("甜度")
                .minSelections(1)
                .maxSelections(1)
                .isActive(true)
                .sortOrder(1);
    }

    // ===== ProductOptionValue =====

    /**
     * 建立預設的產品選項值 Builder (有加價)
     * 預設值: name="加起司", priceAdjustment=10.00, isActive=true, sortOrder=1
     */
    public static ProductOptionValue.ProductOptionValueBuilder defaultOptionValue() {
        return ProductOptionValue.builder()
                .name("加起司")
                .priceAdjustment(new BigDecimal("10.00"))
                .isActive(true)
                .sortOrder(1);
    }

    /**
     * 建立無加價的產品選項值 Builder
     */
    public static ProductOptionValue.ProductOptionValueBuilder freeOptionValue() {
        return ProductOptionValue.builder()
                .name("半糖")
                .priceAdjustment(BigDecimal.ZERO)
                .isActive(true)
                .sortOrder(1);
    }

    // ===== Order =====

    /**
     * 建立草稿狀態的訂單 Builder
     * 預設值: status="DRAFT", orderType="DINE_IN", totalAmount=0
     */
    public static Order.OrderBuilder draftOrder() {
        return Order.builder()
                .status("DRAFT")
                .orderType("DINE_IN")
                .totalAmount(BigDecimal.ZERO);
    }

    /**
     * 建立外帶訂單 Builder
     */
    public static Order.OrderBuilder takeoutOrder() {
        return Order.builder()
                .status("DRAFT")
                .orderType("TAKEOUT")
                .totalAmount(BigDecimal.ZERO);
    }

    /**
     * 建立已完成的訂單 Builder
     */
    public static Order.OrderBuilder completedOrder() {
        return Order.builder()
                .status("COMPLETED")
                .orderType("DINE_IN")
                .totalAmount(BigDecimal.ZERO);
    }

    // ===== SingleOrderItem =====

    /**
     * 建立預設的單點訂單項目 Builder
     * 預設值: productName="測試商品", unitPrice=59.00, quantity=1, optionsAmount=0
     * 注意: subtotal 由 calculateSubtotal() 計算
     */
    public static SingleOrderItem.SingleOrderItemBuilder defaultSingleOrderItem() {
        return SingleOrderItem.builder()
                .productName("測試商品")
                .unitPrice(new BigDecimal("59.00"))
                .quantity(1)
                .optionsAmount(BigDecimal.ZERO);
    }

    // ===== OrderItemOptionDTO =====

    /**
     * 建立預設的訂單項目選項 DTO Builder (有加價)
     * 預設值: groupName="加料", valueName="加起司", priceAdjustment=10.00
     */
    public static OrderItemOptionDTO.OrderItemOptionDTOBuilder defaultItemOption() {
        return OrderItemOptionDTO.builder()
                .groupName("加料")
                .valueName("加起司")
                .priceAdjustment(new BigDecimal("10.00"));
    }

    /**
     * 建立無加價的訂單項目選項 DTO Builder
     */
    public static OrderItemOptionDTO.OrderItemOptionDTOBuilder freeItemOption() {
        return OrderItemOptionDTO.builder()
                .groupName("甜度")
                .valueName("半糖")
                .priceAdjustment(BigDecimal.ZERO);
    }

    /**
     * 建立加蛋選項 DTO Builder
     */
    public static OrderItemOptionDTO.OrderItemOptionDTOBuilder eggOption() {
        return OrderItemOptionDTO.builder()
                .groupName("加料")
                .valueName("加蛋")
                .priceAdjustment(new BigDecimal("15.00"));
    }
}
