package com.morningharvest.erp.invoice.repository;

import com.morningharvest.erp.invoice.entity.InvoiceAllowance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceAllowanceRepository extends JpaRepository<InvoiceAllowance, Long> {

    List<InvoiceAllowance> findByInvoiceIdOrderByIdDesc(Long invoiceId);

    Optional<InvoiceAllowance> findByAllowanceNumber(String allowanceNumber);
}
