package com.morningharvest.erp.product.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.event.EventPublisher;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.product.dto.CreateProductCategoryRequest;
import com.morningharvest.erp.product.dto.ProductCategoryDTO;
import com.morningharvest.erp.product.dto.UpdateProductCategoryRequest;
import com.morningharvest.erp.product.entity.ProductCategory;
import com.morningharvest.erp.product.event.ProductCategoryUpdatedEvent;
import com.morningharvest.erp.product.repository.ProductCategoryRepository;
import com.morningharvest.erp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;
    private final ProductRepository productRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public ProductCategoryDTO createCategory(CreateProductCategoryRequest request) {
        log.info("建立商品分類: {}", request.getName());

        // 驗證名稱不重複
        if (productCategoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("分類名稱已存在: " + request.getName());
        }

        ProductCategory category = ProductCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(true)
                .build();

        ProductCategory saved = productCategoryRepository.save(category);
        log.info("商品分類建立成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public ProductCategoryDTO updateCategory(UpdateProductCategoryRequest request) {
        log.info("更新商品分類, id: {}", request.getId());

        ProductCategory category = productCategoryRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("商品分類不存在: " + request.getId()));

        // 保存更新前的完整資料
        ProductCategoryDTO beforeDTO = toDTO(category);

        // 驗證名稱不重複（排除自己）
        if (productCategoryRepository.existsByNameAndIdNot(request.getName(), request.getId())) {
            throw new IllegalArgumentException("分類名稱已存在: " + request.getName());
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        ProductCategory saved = productCategoryRepository.save(category);
        log.info("商品分類更新成功, id: {}", saved.getId());

        ProductCategoryDTO afterDTO = toDTO(saved);

        // 發布事件（包含更新前後的完整資料）
        eventPublisher.publish(
            new ProductCategoryUpdatedEvent(beforeDTO, afterDTO),
            "分類更新"
        );

        return afterDTO;
    }

    @Transactional
    public void deleteCategory(Long id) {
        log.info("刪除商品分類, id: {}", id);

        if (!productCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("商品分類不存在: " + id);
        }

        // 檢查分類下是否有商品
        if (productRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException("無法刪除分類，該分類下仍有商品");
        }

        productCategoryRepository.deleteById(id);
        log.info("商品分類刪除成功, id: {}", id);
    }

    @Transactional(readOnly = true)
    public ProductCategoryDTO getCategoryById(Long id) {
        log.debug("查詢商品分類, id: {}", id);

        ProductCategory category = productCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品分類不存在: " + id));

        return toDTO(category);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductCategoryDTO> listCategories(PageableRequest pageableRequest, Boolean isActive) {
        log.debug("查詢商品分類列表, page: {}, size: {}, isActive: {}",
                pageableRequest.getPage(), pageableRequest.getSize(), isActive);

        Page<ProductCategory> categoryPage;
        if (isActive != null) {
            categoryPage = productCategoryRepository.findByIsActive(isActive, pageableRequest.toPageable());
        } else {
            categoryPage = productCategoryRepository.findAll(pageableRequest.toPageable());
        }

        Page<ProductCategoryDTO> dtoPage = categoryPage.map(this::toDTO);
        return PageResponse.from(dtoPage);
    }

    @Transactional
    public ProductCategoryDTO activateCategory(Long id) {
        log.info("啟用商品分類, id: {}", id);

        ProductCategory category = productCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品分類不存在: " + id));

        category.setIsActive(true);
        ProductCategory saved = productCategoryRepository.save(category);
        log.info("商品分類啟用成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public ProductCategoryDTO deactivateCategory(Long id) {
        log.info("停用商品分類, id: {}", id);

        ProductCategory category = productCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品分類不存在: " + id));

        category.setIsActive(false);
        ProductCategory saved = productCategoryRepository.save(category);
        log.info("商品分類停用成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    private ProductCategoryDTO toDTO(ProductCategory category) {
        return ProductCategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
