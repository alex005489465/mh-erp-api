package com.morningharvest.erp.supplier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 供應商回應 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDTO {

    private Long id;

    private String code;

    private String name;

    private String shortName;

    private String contactPerson;

    private String phone;

    private String mobile;

    private String fax;

    private String email;

    private String taxId;

    private String address;

    private String paymentTerms;

    private String paymentTermsDisplayName;

    private String bankName;

    private String bankAccount;

    private Boolean isActive;

    private String note;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
