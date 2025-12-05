package com.morningharvest.erp.order.repository;

import com.morningharvest.erp.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByStatus(String status, Pageable pageable);

    Page<Order> findByOrderType(String orderType, Pageable pageable);

    Page<Order> findByStatusAndOrderType(String status, String orderType, Pageable pageable);

    // 時間範圍查詢
    Page<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // 組合查詢：時間範圍 + 狀態
    Page<Order> findByCreatedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, String status, Pageable pageable);

    // 組合查詢：時間範圍 + 類型
    Page<Order> findByCreatedAtBetweenAndOrderType(LocalDateTime start, LocalDateTime end, String orderType, Pageable pageable);

    // 組合查詢：時間範圍 + 狀態 + 類型
    Page<Order> findByCreatedAtBetweenAndStatusAndOrderType(LocalDateTime start, LocalDateTime end, String status, String orderType, Pageable pageable);
}
