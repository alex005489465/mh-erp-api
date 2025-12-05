package com.morningharvest.erp.common.test;

import com.morningharvest.erp.inventorycheck.constant.InventoryCheckStatus;
import com.morningharvest.erp.inventorycheck.entity.InventoryCheck;
import com.morningharvest.erp.inventorycheck.entity.InventoryCheckItem;
import com.morningharvest.erp.invoice.dto.IssueInvoiceRequest;
import com.morningharvest.erp.invoice.entity.Invoice;
import com.morningharvest.erp.invoice.entity.InvoiceItem;
import com.morningharvest.erp.material.constant.MaterialCategory;
import com.morningharvest.erp.material.constant.MaterialUnit;
import com.morningharvest.erp.material.entity.Material;
import com.morningharvest.erp.order.dto.OrderItemOptionDTO;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.entity.SingleOrderItem;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.entity.ProductCategory;
import com.morningharvest.erp.product.entity.ProductOptionGroup;
import com.morningharvest.erp.product.entity.ProductOptionValue;
import com.morningharvest.erp.product.entity.ProductRecipe;
import com.morningharvest.erp.purchase.dto.CreatePurchaseItemRequest;
import com.morningharvest.erp.purchase.dto.CreatePurchaseRequest;
import com.morningharvest.erp.purchase.entity.Purchase;
import com.morningharvest.erp.purchase.entity.PurchaseItem;
import com.morningharvest.erp.supplier.constant.PaymentTerms;
import com.morningharvest.erp.supplier.entity.Supplier;
import com.morningharvest.erp.table.constant.TableStatus;
import com.morningharvest.erp.table.entity.DiningTable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    // ===== Invoice =====

    /**
     * 建立預設的發票 Builder (B2C 電子發票)
     * 預設值: invoiceType="B2C", issueType="ELECTRONIC", status="ISSUED"
     */
    public static Invoice.InvoiceBuilder defaultInvoice() {
        return Invoice.builder()
                .invoiceNumber("AA-00000001")
                .invoiceDate(LocalDate.now())
                .invoicePeriod("11312")
                .invoiceType("B2C")
                .issueType("ELECTRONIC")
                .salesAmount(new BigDecimal("100.00"))
                .taxAmount(new BigDecimal("5.00"))
                .totalAmount(new BigDecimal("105.00"))
                .status("ISSUED")
                .isPrinted(false)
                .printCount(0)
                .isVoided(false)
                .isDonated(false);
    }

    /**
     * 建立已作廢的發票 Builder
     */
    public static Invoice.InvoiceBuilder voidedInvoice() {
        return Invoice.builder()
                .invoiceNumber("AA-00000002")
                .invoiceDate(LocalDate.now())
                .invoicePeriod("11312")
                .invoiceType("B2C")
                .issueType("ELECTRONIC")
                .status("VOID")
                .isVoided(true)
                .voidReason("測試作廢");
    }

    // ===== InvoiceItem =====

    /**
     * 建立預設的發票明細 Builder
     */
    public static InvoiceItem.InvoiceItemBuilder defaultInvoiceItem() {
        return InvoiceItem.builder()
                .sequence(1)
                .description("測試商品")
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal("105.00"))
                .amount(new BigDecimal("105.00"));
    }

    // ===== IssueInvoiceRequest =====

    /**
     * 建立預設的開立發票請求 Builder (B2C)
     */
    public static IssueInvoiceRequest.IssueInvoiceRequestBuilder defaultIssueInvoiceRequest() {
        return IssueInvoiceRequest.builder()
                .invoiceType("B2C")
                .issueType("ELECTRONIC")
                .carrierType("MOBILE_BARCODE")
                .carrierValue("/ABC1234")
                .isDonated(false);
    }

    /**
     * 建立 B2B 開立發票請求 Builder
     */
    public static IssueInvoiceRequest.IssueInvoiceRequestBuilder b2bIssueInvoiceRequest() {
        return IssueInvoiceRequest.builder()
                .invoiceType("B2B")
                .issueType("ELECTRONIC")
                .buyerIdentifier("12345678")
                .buyerName("測試公司")
                .isDonated(false);
    }

    // ===== DiningTable =====

    /**
     * 建立預設的桌位 Builder (空桌)
     * 預設值: tableNumber="A1", capacity=4, status=AVAILABLE, isActive=true
     */
    public static DiningTable.DiningTableBuilder defaultTable() {
        return DiningTable.builder()
                .tableNumber("A1")
                .capacity(4)
                .status(TableStatus.AVAILABLE)
                .isActive(true);
    }

    /**
     * 建立佔用中的桌位 Builder
     */
    public static DiningTable.DiningTableBuilder occupiedTable() {
        return DiningTable.builder()
                .tableNumber("A2")
                .capacity(4)
                .status(TableStatus.OCCUPIED)
                .isActive(true);
    }

    /**
     * 建立停用的桌位 Builder
     */
    public static DiningTable.DiningTableBuilder inactiveTable() {
        return DiningTable.builder()
                .tableNumber("A3")
                .capacity(4)
                .status(TableStatus.AVAILABLE)
                .isActive(false);
    }

    // ===== Material =====

    /**
     * 建立預設的原物料 Builder
     * 預設值: code="M001", name="測試原物料", unit="PIECE", isActive=true
     */
    public static Material.MaterialBuilder defaultMaterial() {
        return Material.builder()
                .code("M001")
                .name("測試原物料")
                .unit(MaterialUnit.PIECE)
                .category(MaterialCategory.OTHER)
                .safeStockQuantity(BigDecimal.TEN)
                .currentStockQuantity(new BigDecimal("50.00"))
                .costPrice(new BigDecimal("25.00"))
                .isActive(true);
    }

    /**
     * 建立停用的原物料 Builder
     */
    public static Material.MaterialBuilder inactiveMaterial() {
        return Material.builder()
                .code("M002")
                .name("停用原物料")
                .unit(MaterialUnit.PACK)
                .category(MaterialCategory.OTHER)
                .safeStockQuantity(BigDecimal.ZERO)
                .currentStockQuantity(BigDecimal.ZERO)
                .costPrice(BigDecimal.ZERO)
                .isActive(false);
    }

    // ===== Supplier =====

    /**
     * 建立預設的供應商 Builder
     * 預設值: code="S001", name="測試供應商", paymentTerms="NET30", isActive=true
     */
    public static Supplier.SupplierBuilder defaultSupplier() {
        return Supplier.builder()
                .code("S001")
                .name("測試供應商")
                .shortName("測試")
                .contactPerson("王小明")
                .phone("02-12345678")
                .mobile("0912-345678")
                .email("test@example.com")
                .taxId("12345678")
                .address("台北市信義區測試路100號")
                .paymentTerms(PaymentTerms.NET30)
                .bankName("測試銀行")
                .bankAccount("1234567890")
                .isActive(true);
    }

    /**
     * 建立停用的供應商 Builder
     */
    public static Supplier.SupplierBuilder inactiveSupplier() {
        return Supplier.builder()
                .code("S002")
                .name("停用供應商")
                .shortName("停用")
                .contactPerson("李小華")
                .paymentTerms(PaymentTerms.COD)
                .isActive(false);
    }

    // ===== Purchase =====

    /**
     * 建立預設的進貨單 Builder (草稿狀態)
     * 預設值: purchaseNumber="PO-20251205-0001", status="DRAFT"
     */
    public static Purchase.PurchaseBuilder defaultPurchase() {
        return Purchase.builder()
                .purchaseNumber("PO-20251205-0001")
                .supplierId(1L)
                .supplierName("測試供應商")
                .status("DRAFT")
                .totalAmount(new BigDecimal("1000.00"))
                .purchaseDate(LocalDate.now());
    }

    /**
     * 建立已確認的進貨單 Builder
     */
    public static Purchase.PurchaseBuilder confirmedPurchase() {
        return defaultPurchase()
                .status("CONFIRMED")
                .confirmedAt(LocalDateTime.now())
                .confirmedBy("admin");
    }

    // ===== PurchaseItem =====

    /**
     * 建立預設的進貨明細 Builder
     */
    public static PurchaseItem.PurchaseItemBuilder defaultPurchaseItem() {
        return PurchaseItem.builder()
                .materialId(1L)
                .materialCode("M001")
                .materialName("測試原物料")
                .materialUnit("PIECE")
                .quantity(new BigDecimal("10.00"))
                .unitPrice(new BigDecimal("100.00"))
                .subtotal(new BigDecimal("1000.00"));
    }

    // ===== CreatePurchaseRequest =====

    /**
     * 建立預設的進貨單建立請求 Builder
     */
    public static CreatePurchaseRequest.CreatePurchaseRequestBuilder defaultCreatePurchaseRequest() {
        return CreatePurchaseRequest.builder()
                .supplierId(1L)
                .purchaseDate(LocalDate.now())
                .items(List.of(defaultCreatePurchaseItemRequest().build()));
    }

    /**
     * 建立預設的進貨明細建立請求 Builder
     */
    public static CreatePurchaseItemRequest.CreatePurchaseItemRequestBuilder defaultCreatePurchaseItemRequest() {
        return CreatePurchaseItemRequest.builder()
                .materialId(1L)
                .quantity(new BigDecimal("10.00"))
                .unitPrice(new BigDecimal("100.00"));
    }

    // ===== InventoryCheck =====

    /**
     * 建立預設的盤點單 Builder (計畫中狀態)
     * 預設值: checkNumber="IC-20251205-0001", status="PLANNED"
     */
    public static InventoryCheck.InventoryCheckBuilder defaultInventoryCheck() {
        return InventoryCheck.builder()
                .checkNumber("IC-20251205-0001")
                .status(InventoryCheckStatus.PLANNED)
                .checkDate(LocalDate.now())
                .totalItems(0)
                .totalDifferenceAmount(BigDecimal.ZERO);
    }

    /**
     * 建立盤點中的盤點單 Builder
     */
    public static InventoryCheck.InventoryCheckBuilder inProgressInventoryCheck() {
        return defaultInventoryCheck()
                .status(InventoryCheckStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .startedBy("admin");
    }

    /**
     * 建立已確認的盤點單 Builder
     */
    public static InventoryCheck.InventoryCheckBuilder confirmedInventoryCheck() {
        return inProgressInventoryCheck()
                .status(InventoryCheckStatus.CONFIRMED)
                .confirmedAt(LocalDateTime.now())
                .confirmedBy("admin");
    }

    // ===== InventoryCheckItem =====

    /**
     * 建立預設的盤點明細 Builder (未盤點)
     * 預設值: systemQuantity=50.00, unitCost=25.00, isChecked=false
     */
    public static InventoryCheckItem.InventoryCheckItemBuilder defaultInventoryCheckItem() {
        return InventoryCheckItem.builder()
                .inventoryCheckId(1L)
                .materialId(1L)
                .materialCode("M001")
                .materialName("測試原物料")
                .materialUnit("PIECE")
                .systemQuantity(new BigDecimal("50.00"))
                .unitCost(new BigDecimal("25.00"))
                .isChecked(false);
    }

    /**
     * 建立已盤點的盤點明細 Builder (盤虧)
     * 實際數量 48，系統數量 50，盤差 -2
     */
    public static InventoryCheckItem.InventoryCheckItemBuilder checkedInventoryCheckItem() {
        return defaultInventoryCheckItem()
                .actualQuantity(new BigDecimal("48.00"))
                .differenceQuantity(new BigDecimal("-2.00"))
                .differenceAmount(new BigDecimal("-50.00"))
                .isChecked(true);
    }

    /**
     * 建立已盤點的盤點明細 Builder (盤盈)
     * 實際數量 55，系統數量 50，盤差 +5
     */
    public static InventoryCheckItem.InventoryCheckItemBuilder surplusInventoryCheckItem() {
        return defaultInventoryCheckItem()
                .actualQuantity(new BigDecimal("55.00"))
                .differenceQuantity(new BigDecimal("5.00"))
                .differenceAmount(new BigDecimal("125.00"))
                .isChecked(true);
    }

    // ===== ProductRecipe =====

    /**
     * 建立預設的商品配方 Builder
     * 預設值: productId=1, materialId=1, quantity=1.0000, unit="PIECE"
     */
    public static ProductRecipe.ProductRecipeBuilder defaultProductRecipe() {
        return ProductRecipe.builder()
                .productId(1L)
                .productName("測試商品")
                .materialId(1L)
                .materialCode("M001")
                .materialName("測試原物料")
                .quantity(new BigDecimal("1.0000"))
                .unit("PIECE")
                .note("測試備註");
    }

    /**
     * 建立無備註的商品配方 Builder
     */
    public static ProductRecipe.ProductRecipeBuilder simpleProductRecipe() {
        return ProductRecipe.builder()
                .productId(1L)
                .productName("測試商品")
                .materialId(1L)
                .materialCode("M001")
                .materialName("測試原物料")
                .quantity(new BigDecimal("0.5000"))
                .unit("KILOGRAM");
    }
}
