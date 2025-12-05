package com.morningharvest.erp.product.repository;

import com.morningharvest.erp.product.entity.ProductRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRecipeRepository extends JpaRepository<ProductRecipe, Long> {

    /**
     * 依商品 ID 查詢配方清單
     */
    List<ProductRecipe> findByProductId(Long productId);

    /**
     * 依原物料 ID 查詢使用該原物料的配方
     */
    List<ProductRecipe> findByMaterialId(Long materialId);

    /**
     * 檢查商品是否已有該原物料的配方
     */
    boolean existsByProductIdAndMaterialId(Long productId, Long materialId);

    /**
     * 檢查商品是否已有該原物料的配方（排除指定 ID）
     */
    boolean existsByProductIdAndMaterialIdAndIdNot(Long productId, Long materialId, Long id);

    /**
     * 依商品 ID 刪除所有配方
     */
    void deleteByProductId(Long productId);

    /**
     * 依商品 ID 更新商品名稱
     */
    @Modifying
    @Query("UPDATE ProductRecipe pr SET pr.productName = :productName WHERE pr.productId = :productId")
    void updateProductNameByProductId(@Param("productId") Long productId, @Param("productName") String productName);

    /**
     * 依原物料 ID 更新原物料資訊
     */
    @Modifying
    @Query("UPDATE ProductRecipe pr SET pr.materialCode = :materialCode, pr.materialName = :materialName, pr.unit = :unit WHERE pr.materialId = :materialId")
    void updateMaterialInfoByMaterialId(
            @Param("materialId") Long materialId,
            @Param("materialCode") String materialCode,
            @Param("materialName") String materialName,
            @Param("unit") String unit);

    /**
     * 統計商品的配方數量
     */
    long countByProductId(Long productId);

    /**
     * 統計使用某原物料的商品數量
     */
    long countByMaterialId(Long materialId);
}
