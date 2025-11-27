package com.morningharvest.erp.product.repository;

import com.morningharvest.erp.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Page<Product> findByIsActive(Boolean isActive, Pageable pageable);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByCategoryIdAndIsActive(Long categoryId, Boolean isActive, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    /**
     * 批量更新指定分類下所有商品的分類名稱
     *
     * @param categoryId 分類 ID
     * @param newCategoryName 新的分類名稱
     * @return 更新的商品數量
     */
    @Modifying
    @Query("UPDATE Product p SET p.categoryName = :newCategoryName WHERE p.categoryId = :categoryId")
    int updateCategoryNameByCategoryId(@Param("categoryId") Long categoryId, @Param("newCategoryName") String newCategoryName);
}
