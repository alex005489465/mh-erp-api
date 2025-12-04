package com.morningharvest.erp.invoice.repository;

import com.morningharvest.erp.invoice.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByOrderId(Long orderId);

    List<Invoice> findByOrderIdOrderByIdDesc(Long orderId);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Page<Invoice> findByStatus(String status, Pageable pageable);

    Page<Invoice> findByInvoiceDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<Invoice> findByInvoiceDateBetweenAndStatus(
            LocalDate startDate, LocalDate endDate, String status, Pageable pageable);

    boolean existsByOrderId(Long orderId);
}
