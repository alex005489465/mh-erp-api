package com.morningharvest.erp.table.repository;

import com.morningharvest.erp.table.entity.DiningTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    List<DiningTable> findByIsActiveTrueOrderByTableNumberAsc();

    List<DiningTable> findByStatusAndIsActiveTrueOrderByTableNumberAsc(String status);

    Page<DiningTable> findByIsActiveTrue(Pageable pageable);

    Page<DiningTable> findByStatusAndIsActiveTrue(String status, Pageable pageable);

    Optional<DiningTable> findByTableNumberAndIsActiveTrue(String tableNumber);

    boolean existsByTableNumberAndIsActiveTrue(String tableNumber);

    boolean existsByTableNumberAndIdNotAndIsActiveTrue(String tableNumber, Long id);

    Optional<DiningTable> findByCurrentOrderId(Long orderId);
}
