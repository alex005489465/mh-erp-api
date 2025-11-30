package com.morningharvest.erp.order.repository;

import com.morningharvest.erp.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);

    /**
     * 取得訂單內的最大 group_sequence
     */
    @Query("SELECT MAX(oi.groupSequence) FROM OrderItem oi WHERE oi.orderId = :orderId")
    Integer findMaxGroupSequenceByOrderId(@Param("orderId") Long orderId);

    /**
     * 依訂單 ID 和 group_sequence 查詢項目 (用於套餐刪除)
     */
    List<OrderItem> findByOrderIdAndGroupSequence(Long orderId, Integer groupSequence);

    /**
     * 依訂單 ID 和 group_sequence 刪除項目 (整組套餐刪除)
     */
    void deleteByOrderIdAndGroupSequence(Long orderId, Integer groupSequence);
}
