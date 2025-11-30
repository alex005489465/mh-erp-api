package com.morningharvest.erp.pos.service;

import com.morningharvest.erp.combo.entity.Combo;
import com.morningharvest.erp.combo.entity.ComboItem;
import com.morningharvest.erp.combo.repository.ComboItemRepository;
import com.morningharvest.erp.combo.repository.ComboRepository;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.pos.dto.ComboItemInfoDTO;
import com.morningharvest.erp.pos.dto.SaleItemDTO;
import com.morningharvest.erp.pos.dto.SaleItemPayloadDTO;
import com.morningharvest.erp.product.dto.ProductOptionGroupDetailDTO;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.repository.ProductRepository;
import com.morningharvest.erp.product.service.ProductOptionGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosMenuService {

    private final ProductRepository productRepository;
    private final ComboRepository comboRepository;
    private final ComboItemRepository comboItemRepository;
    private final ProductOptionGroupService productOptionGroupService;

    /**
     * 查詢銷售物品列表（商品 + 套餐混合）
     *
     * @param categoryId 分類 ID（可選）
     * @return 銷售物品列表
     */
    @Transactional(readOnly = true)
    public List<SaleItemDTO> listSaleItems(Long categoryId) {
        log.debug("查詢銷售物品列表, categoryId: {}", categoryId);

        List<SaleItemDTO> result = new ArrayList<>();

        // 使用大頁面取得所有資料
        Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.ASC, "sortOrder"));

        // 查詢上架商品
        List<Product> products;
        if (categoryId != null) {
            products = productRepository.findByCategoryIdAndIsActive(categoryId, true, pageable).getContent();
        } else {
            products = productRepository.findByIsActive(true, pageable).getContent();
        }

        // 轉換商品為 SaleItem
        for (Product product : products) {
            result.add(toSingleSaleItem(product));
        }

        // 查詢啟用套餐
        List<Combo> combos;
        if (categoryId != null) {
            combos = comboRepository.findByCategoryIdAndIsActive(categoryId, true, pageable).getContent();
        } else {
            combos = comboRepository.findByIsActive(true, pageable).getContent();
        }

        // 轉換套餐為 SaleItem
        for (Combo combo : combos) {
            result.add(toComboSaleItem(combo));
        }

        // 依 sortOrder 排序
        result.sort(Comparator.comparing(SaleItemDTO::getSortOrder));

        log.debug("查詢銷售物品列表完成, 共 {} 筆", result.size());
        return result;
    }

    /**
     * 查詢單一銷售物品詳情
     *
     * @param type 類型：SINGLE 或 COMBO
     * @param id   ID
     * @return 銷售物品詳情
     */
    @Transactional(readOnly = true)
    public SaleItemDTO getSaleItemDetail(String type, Long id) {
        log.debug("查詢銷售物品詳情, type: {}, id: {}", type, id);

        if ("SINGLE".equals(type)) {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("商品不存在: " + id));
            return toSingleSaleItemWithOptions(product);
        } else if ("COMBO".equals(type)) {
            Combo combo = comboRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("套餐不存在: " + id));
            return toComboSaleItemWithOptions(combo);
        } else {
            throw new IllegalArgumentException("無效的類型: " + type);
        }
    }

    /**
     * 轉換商品為 SaleItem（列表用，不含選項詳情）
     */
    private SaleItemDTO toSingleSaleItem(Product product) {
        return SaleItemDTO.builder()
                .type("SINGLE")
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .categoryId(product.getCategoryId())
                .categoryName(product.getCategoryName())
                .sortOrder(product.getSortOrder())
                .orderPayload(List.of(
                        SaleItemPayloadDTO.single(product.getId(), product.getName(), product.getPrice())
                ))
                .build();
    }

    /**
     * 轉換商品為 SaleItem（詳情用，含選項）
     */
    private SaleItemDTO toSingleSaleItemWithOptions(Product product) {
        List<ProductOptionGroupDetailDTO> optionGroups =
                productOptionGroupService.listGroupsWithValuesByProductId(product.getId());

        return SaleItemDTO.builder()
                .type("SINGLE")
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .categoryId(product.getCategoryId())
                .categoryName(product.getCategoryName())
                .sortOrder(product.getSortOrder())
                .optionGroups(optionGroups)
                .orderPayload(List.of(
                        SaleItemPayloadDTO.single(product.getId(), product.getName(), product.getPrice())
                ))
                .build();
    }

    /**
     * 轉換套餐為 SaleItem（列表用，不含選項詳情）
     */
    private SaleItemDTO toComboSaleItem(Combo combo) {
        List<ComboItem> comboItems = comboItemRepository.findByComboIdOrderBySortOrder(combo.getId());

        List<SaleItemPayloadDTO> payload = buildComboPayload(combo, comboItems);

        return SaleItemDTO.builder()
                .type("COMBO")
                .id(combo.getId())
                .name(combo.getName())
                .description(combo.getDescription())
                .price(combo.getPrice())
                .imageUrl(combo.getImageUrl())
                .categoryId(combo.getCategoryId())
                .categoryName(combo.getCategoryName())
                .sortOrder(combo.getSortOrder())
                .orderPayload(payload)
                .build();
    }

    /**
     * 轉換套餐為 SaleItem（詳情用，含選項）
     */
    private SaleItemDTO toComboSaleItemWithOptions(Combo combo) {
        List<ComboItem> comboItems = comboItemRepository.findByComboIdOrderBySortOrder(combo.getId());

        // 批次載入所有商品的選項群組
        List<Long> productIds = comboItems.stream()
                .map(ComboItem::getProductId)
                .distinct()
                .toList();

        Map<Long, List<ProductOptionGroupDetailDTO>> optionsByProductId = productIds.stream()
                .collect(Collectors.toMap(
                        productId -> productId,
                        productOptionGroupService::listGroupsWithValuesByProductId
                ));

        // 建立套餐內商品資訊
        List<ComboItemInfoDTO> items = comboItems.stream()
                .map(item -> ComboItemInfoDTO.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .sortOrder(item.getSortOrder())
                        .optionGroups(optionsByProductId.getOrDefault(item.getProductId(), List.of()))
                        .build())
                .toList();

        List<SaleItemPayloadDTO> payload = buildComboPayload(combo, comboItems);

        return SaleItemDTO.builder()
                .type("COMBO")
                .id(combo.getId())
                .name(combo.getName())
                .description(combo.getDescription())
                .price(combo.getPrice())
                .imageUrl(combo.getImageUrl())
                .categoryId(combo.getCategoryId())
                .categoryName(combo.getCategoryName())
                .sortOrder(combo.getSortOrder())
                .items(items)
                .orderPayload(payload)
                .build();
    }

    /**
     * 建立套餐的 orderPayload
     */
    private List<SaleItemPayloadDTO> buildComboPayload(Combo combo, List<ComboItem> comboItems) {
        List<SaleItemPayloadDTO> payload = new ArrayList<>();

        // 套餐標頭
        payload.add(SaleItemPayloadDTO.comboHeader(combo.getId(), combo.getName(), combo.getPrice()));

        // 套餐內商品
        for (ComboItem item : comboItems) {
            payload.add(SaleItemPayloadDTO.comboItem(
                    combo.getId(),
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity()
            ));
        }

        return payload;
    }
}
