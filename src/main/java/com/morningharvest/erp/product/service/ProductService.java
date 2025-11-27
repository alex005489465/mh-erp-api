package com.morningharvest.erp.product.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.product.dto.CreateProductRequest;
import com.morningharvest.erp.product.dto.ProductDTO;
import com.morningharvest.erp.product.dto.UpdateProductRequest;
import com.morningharvest.erp.product.entity.Product;
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

    @Transactional
    public ProductDTO createProduct(CreateProductRequest request) {
        log.info("建立商品: {}", request.getName());

        // 驗證名稱不重複
        if (productRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("商品名稱已存在: " + request.getName());
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
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

        // 驗證名稱不重複（排除自己）
        if (productRepository.existsByNameAndIdNot(request.getName(), request.getId())) {
            throw new IllegalArgumentException("商品名稱已存在: " + request.getName());
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        if (request.getSortOrder() != null) {
            product.setSortOrder(request.getSortOrder());
        }

        Product saved = productRepository.save(product);
        log.info("商品更新成功, id: {}", saved.getId());

        return toDTO(saved);
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
    public PageResponse<ProductDTO> listProducts(PageableRequest pageableRequest, Boolean isActive) {
        log.debug("查詢商品列表, page: {}, size: {}, isActive: {}",
                pageableRequest.getPage(), pageableRequest.getSize(), isActive);

        Page<Product> productPage;
        if (isActive != null) {
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

    private ProductDTO toDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .isActive(product.getIsActive())
                .sortOrder(product.getSortOrder())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
