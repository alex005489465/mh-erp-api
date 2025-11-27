package com.morningharvest.erp.option.repository;

import com.morningharvest.erp.option.entity.OptionTemplateGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OptionTemplateGroupRepository extends JpaRepository<OptionTemplateGroup, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Page<OptionTemplateGroup> findByIsActive(Boolean isActive, Pageable pageable);
}
