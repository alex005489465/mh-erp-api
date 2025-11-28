package com.morningharvest.erp.product.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.event.EventPublisher;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.product.dto.CreateProductRequest;
import com.morningharvest.erp.product.dto.ProductDTO;
import com.morningharvest.erp.product.dto.UpdateProductRequest;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.entity.ProductCategory;
import com.morningharvest.erp.product.event.ProductUpdatedEvent;
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
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public ProductDTO createProduct(CreateProductRequest request) {
        log.info("建立商品: {}", request.getName());

        // 驗證名稱不重複
        if (productRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("商品名稱已存在: " + request.getName());
        }

        // 驗證分類（如果有指定）
        Long categoryId = request.getCategoryId();
        String categoryName = request.getCategoryName();
        validateCategory(categoryId, categoryName);

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .categoryId(categoryId)
                .categoryName(categoryName)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(true)
                .build();

        Product saved = productRepository.save(product);
        log.info("商品建立成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public ProductDTO updateProduct(UpdateProductRequest request) {
        log.info("更新商品, id: {}", request.getId());

        Product product = productRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在: " + request.getId()));

        // 保存更新前的完整資料
        ProductDTO beforeDTO = toDTO(product);

        // 驗證名稱不重複（排除自己）
        if (productRepository.existsByNameAndIdNot(request.getName(), request.getId())) {
            throw new IllegalArgumentException("商品名稱已存在: " + request.getName());
        }

        // 驗證分類（如果有指定）
        Long categoryId = request.getCategoryId();
        String categoryName = request.getCategoryName();
        validateCategory(categoryId, categoryName);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategoryId(categoryId);
        product.setCategoryName(categoryName);
        if (request.getSortOrder() != null) {
            product.setSortOrder(request.getSortOrder());
        }

        Product saved = productRepository.save(product);
        log.info("商品更新成功, id: {}", saved.getId());

        ProductDTO afterDTO = toDTO(saved);

        // 發布商品更新事件（包含更新前後的完整資料）
        eventPublisher.publish(
            new ProductUpdatedEvent(beforeDTO, afterDTO),
            "商品更新"
        );

        return afterDTO;
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.info("刪除商品, id: {}", id);

        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("商品不存在: " + id);
        }

        productRepository.deleteById(id);
        log.info("商品刪除成功, id: {}", id);
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        log.debug("查詢商品, id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在: " + id));

        return toDTO(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDTO> listProducts(PageableRequest pageableRequest, Boolean isActive, Long categoryId) {
        log.debug("查詢商品列表, page: {}, size: {}, isActive: {}, categoryId: {}",
                pageableRequest.getPage(), pageableRequest.getSize(), isActive, categoryId);

        Page<Product> productPage;

        if (categoryId != null && isActive != null) {
            productPage = productRepository.findByCategoryIdAndIsActive(categoryId, isActive, pageableRequest.toPageable());
        } else if (categoryId != null) {
            productPage = productRepository.findByCategoryId(categoryId, pageableRequest.toPageable());
        } else if (isActive != null) {
            productPage = productRepository.findByIsActive(isActive, pageableRequest.toPageable());
        } else {
            productPage = productRepository.findAll(pageableRequest.toPageable());
        }

        Page<ProductDTO> dtoPage = productPage.map(this::toDTO);
        return PageResponse.from(dtoPage);
    }

    @Transactional
    public ProductDTO activateProduct(Long id) {
        log.info("上架商品, id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在: " + id));

        product.setIsActive(true);
        Product saved = productRepository.save(product);
        log.info("商品上架成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public ProductDTO deactivateProduct(Long id) {
        log.info("下架商品, id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在: " + id));

        product.setIsActive(false);
        Product saved = productRepository.save(product);
        log.info("商品下架成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    /**
     * 驗證分類 ID 與名稱的一致性
     *
     * @param categoryId   分類 ID（可為 null）
     * @param categoryName 分類名稱（可為 null）
     */
    private void validateCategory(Long categoryId, String categoryName) {
        // 兩者都為 null，表示無分類
        if (categoryId == null && categoryName == null) {
            return;
        }

        // 只有其中一個為 null，資料不一致
        if (categoryId == null || categoryName == null) {
            throw new IllegalArgumentException("分類 ID 與名稱必須同時提供或同時為空");
        }

        // 驗證分類是否存在，並檢查名稱是否一致
        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("商品分類不存在: " + categoryId));

        if (!category.getName().equals(categoryName)) {
            throw new IllegalArgumentException("分類名稱不一致: 預期 " + category.getName() + "，收到 " + categoryName);
        }
    }

    private ProductDTO toDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .categoryId(product.getCategoryId())
                .categoryName(product.getCategoryName())  // 使用冗餘欄位
                .isActive(product.getIsActive())
                .sortOrder(product.getSortOrder())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
