package com.morningharvest.erp.product.repository;

import com.morningharvest.erp.product.entity.ProductOptionValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductOptionValueRepository extends JpaRepository<ProductOptionValue, Long> {

    List<ProductOptionValue> findByGroupIdOrderBySortOrder(Long groupId);

    List<ProductOptionValue> findByGroupIdAndIsActiveOrderBySortOrder(Long groupId, Boolean isActive);

    boolean existsByGroupIdAndName(Long groupId, String name);

    boolean existsByGroupIdAndNameAndIdNot(Long groupId, String name, Long id);

    void deleteByGroupId(Long groupId);

    int countByGroupId(Long groupId);

    @Query("SELECT v FROM ProductOptionValue v WHERE v.groupId IN :groupIds ORDER BY v.groupId, v.sortOrder")
    List<ProductOptionValue> findByGroupIdInOrderByGroupIdAndSortOrder(@Param("groupIds") List<Long> groupIds);
}
