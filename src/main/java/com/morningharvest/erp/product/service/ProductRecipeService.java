package com.morningharvest.erp.product.service;

import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.material.entity.Material;
import com.morningharvest.erp.material.repository.MaterialRepository;
import com.morningharvest.erp.product.dto.CreateProductRecipeRequest;
import com.morningharvest.erp.product.dto.ProductRecipeDTO;
import com.morningharvest.erp.product.dto.UpdateProductRecipeRequest;
import com.morningharvest.erp.product.entity.Product;
import com.morningharvest.erp.product.entity.ProductRecipe;
import com.morningharvest.erp.product.repository.ProductRecipeRepository;
import com.morningharvest.erp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductRecipeService {

    private final ProductRecipeRepository productRecipeRepository;
    private final ProductRepository productRepository;
    private final MaterialRepository materialRepository;

    @Transactional
    public ProductRecipeDTO createRecipe(CreateProductRecipeRequest request) {
        log.info("新增配方, productId: {}, materialId: {}", request.getProductId(), request.getMaterialId());

        // 驗證商品存在
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在: " + request.getProductId()));

        // 驗證原物料存在
        Material material = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> new ResourceNotFoundException("原物料不存在: " + request.getMaterialId()));

        // 驗證不重複（同一商品不能有重複的原物料）
        if (productRecipeRepository.existsByProductIdAndMaterialId(request.getProductId(), request.getMaterialId())) {
            throw new IllegalArgumentException("此商品已有該原物料的配方");
        }

        ProductRecipe recipe = ProductRecipe.builder()
                .productId(product.getId())
                .productName(product.getName())
                .materialId(material.getId())
                .materialCode(material.getCode())
                .materialName(material.getName())
                .quantity(request.getQuantity())
                .unit(material.getUnit())
                .note(request.getNote())
                .build();

        ProductRecipe saved = productRecipeRepository.save(recipe);
        log.info("配方新增成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public ProductRecipeDTO updateRecipe(UpdateProductRecipeRequest request) {
        log.info("更新配方, id: {}", request.getId());

        ProductRecipe recipe = productRecipeRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("配方不存在: " + request.getId()));

        recipe.setQuantity(request.getQuantity());
        recipe.setNote(request.getNote());

        ProductRecipe saved = productRecipeRepository.save(recipe);
        log.info("配方更新成功, id: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional
    public void deleteRecipe(Long id) {
        log.info("刪除配方, id: {}", id);

        if (!productRecipeRepository.existsById(id)) {
            throw new ResourceNotFoundException("配方不存在: " + id);
        }

        productRecipeRepository.deleteById(id);
        log.info("配方刪除成功, id: {}", id);
    }

    @Transactional(readOnly = true)
    public ProductRecipeDTO getRecipeById(Long id) {
        log.debug("查詢配方, id: {}", id);

        ProductRecipe recipe = productRecipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("配方不存在: " + id));

        return toDTO(recipe);
    }

    @Transactional(readOnly = true)
    public List<ProductRecipeDTO> listRecipesByProductId(Long productId) {
        log.debug("查詢商品配方清單, productId: {}", productId);

        // 驗證商品存在
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("商品不存在: " + productId);
        }

        List<ProductRecipe> recipes = productRecipeRepository.findByProductId(productId);
        return recipes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductRecipeDTO> listRecipesByMaterialId(Long materialId) {
        log.debug("查詢原物料使用清單, materialId: {}", materialId);

        // 驗證原物料存在
        if (!materialRepository.existsById(materialId)) {
            throw new ResourceNotFoundException("原物料不存在: " + materialId);
        }

        List<ProductRecipe> recipes = productRecipeRepository.findByMaterialId(materialId);
        return recipes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private ProductRecipeDTO toDTO(ProductRecipe recipe) {
        return ProductRecipeDTO.builder()
                .id(recipe.getId())
                .productId(recipe.getProductId())
                .productName(recipe.getProductName())
                .materialId(recipe.getMaterialId())
                .materialCode(recipe.getMaterialCode())
                .materialName(recipe.getMaterialName())
                .quantity(recipe.getQuantity())
                .unit(recipe.getUnit())
                .note(recipe.getNote())
                .createdAt(recipe.getCreatedAt())
                .updatedAt(recipe.getUpdatedAt())
                .build();
    }
}
