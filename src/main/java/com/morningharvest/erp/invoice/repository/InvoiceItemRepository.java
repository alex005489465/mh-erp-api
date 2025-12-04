package com.morningharvest.erp.invoice.repository;

import com.morningharvest.erp.invoice.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    List<InvoiceItem> findByInvoiceIdOrderBySequenceAsc(Long invoiceId);

    @Modifying
    void deleteByInvoiceId(Long invoiceId);
}
