package com.morningharvest.erp.material.repository;

import com.morningharvest.erp.material.entity.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    /**
     * 檢查編號是否存在
     */
    boolean existsByCode(String code);

    /**
     * 檢查編號是否存在（排除指定 ID）
     */
    boolean existsByCodeAndIdNot(String code, Long id);

    /**
     * 檢查名稱是否存在
     */
    boolean existsByName(String name);

    /**
     * 檢查名稱是否存在（排除指定 ID）
     */
    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * 依啟用狀態查詢
     */
    Page<Material> findByIsActive(Boolean isActive, Pageable pageable);

    /**
     * 依分類查詢
     */
    Page<Material> findByCategory(String category, Pageable pageable);

    /**
     * 依分類和啟用狀態查詢
     */
    Page<Material> findByCategoryAndIsActive(String category, Boolean isActive, Pageable pageable);

    /**
     * 依名稱模糊查詢
     */
    Page<Material> findByNameContaining(String name, Pageable pageable);

    /**
     * 依名稱模糊查詢且啟用狀態篩選
     */
    Page<Material> findByNameContainingAndIsActive(String name, Boolean isActive, Pageable pageable);

    /**
     * 依分類、名稱模糊、啟用狀態查詢
     */
    Page<Material> findByCategoryAndNameContainingAndIsActive(
            String category, String name, Boolean isActive, Pageable pageable);

    /**
     * 依分類和名稱模糊查詢
     */
    Page<Material> findByCategoryAndNameContaining(String category, String name, Pageable pageable);
}
