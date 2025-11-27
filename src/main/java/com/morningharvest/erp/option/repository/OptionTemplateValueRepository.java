package com.morningharvest.erp.option.repository;

import com.morningharvest.erp.option.entity.OptionTemplateValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionTemplateValueRepository extends JpaRepository<OptionTemplateValue, Long> {

    List<OptionTemplateValue> findByGroupIdOrderBySortOrder(Long groupId);

    List<OptionTemplateValue> findByGroupIdAndIsActiveOrderBySortOrder(Long groupId, Boolean isActive);

    boolean existsByGroupIdAndName(Long groupId, String name);

    boolean existsByGroupIdAndNameAndIdNot(Long groupId, String name, Long id);

    void deleteByGroupId(Long groupId);

    int countByGroupId(Long groupId);
}
