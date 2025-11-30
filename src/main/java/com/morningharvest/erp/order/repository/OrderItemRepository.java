package com.morningharvest.erp.order.repository;

import com.morningharvest.erp.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 訂單項目 Repository
 * 支援多型查詢 (STI)
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 依訂單 ID 查詢所有項目（按 ID 排序）
     */
    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    /**
     * 依訂單 ID 刪除所有項目
     */
    @Modifying
    void deleteByOrderId(Long orderId);
}
