package com.morningharvest.erp.product.repository;

import com.morningharvest.erp.product.entity.ProductOptionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductOptionGroupRepository extends JpaRepository<ProductOptionGroup, Long> {

    List<ProductOptionGroup> findByProductIdOrderBySortOrder(Long productId);

    List<ProductOptionGroup> findByProductIdAndIsActiveOrderBySortOrder(Long productId, Boolean isActive);

    boolean existsByProductIdAndName(Long productId, String name);

    boolean existsByProductIdAndNameAndIdNot(Long productId, String name, Long id);

    void deleteByProductId(Long productId);

    int countByProductId(Long productId);
}
