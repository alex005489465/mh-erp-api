package com.morningharvest.erp.payment.repository;

import com.morningharvest.erp.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByOrderIdOrderByIdDesc(Long orderId);

    boolean existsByOrderIdAndStatus(Long orderId, String status);
}
