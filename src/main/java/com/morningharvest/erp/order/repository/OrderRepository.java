package com.morningharvest.erp.order.repository;

import com.morningharvest.erp.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByStatus(String status, Pageable pageable);

    Page<Order> findByOrderType(String orderType, Pageable pageable);

    Page<Order> findByStatusAndOrderType(String status, String orderType, Pageable pageable);
}
