package com.morningharvest.erp.combo.repository;

import com.morningharvest.erp.combo.entity.ComboItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComboItemRepository extends JpaRepository<ComboItem, Long> {

    List<ComboItem> findByComboIdOrderBySortOrder(Long comboId);

    boolean existsByComboIdAndProductId(Long comboId, Long productId);

    boolean existsByComboIdAndProductIdAndIdNot(Long comboId, Long productId, Long id);

    void deleteByComboId(Long comboId);

    int countByComboId(Long comboId);

    boolean existsByProductId(Long productId);

    @Modifying
    @Query("UPDATE ComboItem ci SET ci.productName = :newProductName WHERE ci.productId = :productId")
    int updateProductNameByProductId(@Param("productId") Long productId, @Param("newProductName") String newProductName);
}
