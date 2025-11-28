package com.morningharvest.erp.combo.repository;

import com.morningharvest.erp.combo.entity.Combo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ComboRepository extends JpaRepository<Combo, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Page<Combo> findByIsActive(Boolean isActive, Pageable pageable);

    Page<Combo> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Combo> findByCategoryIdAndIsActive(Long categoryId, Boolean isActive, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    @Modifying
    @Query("UPDATE Combo c SET c.categoryName = :newCategoryName WHERE c.categoryId = :categoryId")
    int updateCategoryNameByCategoryId(@Param("categoryId") Long categoryId, @Param("newCategoryName") String newCategoryName);
}
