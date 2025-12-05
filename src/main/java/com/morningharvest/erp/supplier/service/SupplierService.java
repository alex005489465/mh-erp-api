package com.morningharvest.erp.supplier.service;

import com.morningharvest.erp.common.dto.PageResponse;
import com.morningharvest.erp.common.dto.PageableRequest;
import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.supplier.constant.PaymentTerms;
import com.morningharvest.erp.supplier.dto.CreateSupplierRequest;
import com.morningharvest.erp.supplier.dto.SupplierDTO;
import com.morningharvest.erp.supplier.dto.UpdateSupplierRequest;
import com.morningharvest.erp.supplier.entity.Supplier;
import com.morningharvest.erp.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 供應商業務邏輯層
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService {

    private final SupplierRepository supplierRepository;

    /**
     * 建立供應商
     */
    @Transactional
    public SupplierDTO createSupplier(CreateSupplierRequest request) {
        // 驗證供應商編號唯一性
        if (supplierRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("供應商編號已存在: " + request.getCode());
        }

        Supplier supplier = Supplier.builder()
                .code(request.getCode())
                .name(request.getName())
                .shortName(request.getShortName())
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone())
                .mobile(request.getMobile())
                .fax(request.getFax())
                .email(request.getEmail())
                .taxId(request.getTaxId())
                .address(request.getAddress())
                .paymentTerms(request.getPaymentTerms())
                .bankName(request.getBankName())
                .bankAccount(request.getBankAccount())
                .note(request.getNote())
                .isActive(true)
                .build();

        Supplier saved = supplierRepository.save(supplier);
        log.info("供應商建立成功, id: {}, code: {}", saved.getId(), saved.getCode());

        return toDTO(saved);
    }

    /**
     * 更新供應商
     */
    @Transactional
    public SupplierDTO updateSupplier(UpdateSupplierRequest request) {
        Supplier supplier = supplierRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("供應商不存在, id: " + request.getId()));

        // 驗證供應商編號唯一性（排除自己）
        if (supplierRepository.existsByCodeAndIdNot(request.getCode(), request.getId())) {
            throw new IllegalArgumentException("供應商編號已存在: " + request.getCode());
        }

        supplier.setCode(request.getCode());
        supplier.setName(request.getName());
        supplier.setShortName(request.getShortName());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setPhone(request.getPhone());
        supplier.setMobile(request.getMobile());
        supplier.setFax(request.getFax());
        supplier.setEmail(request.getEmail());
        supplier.setTaxId(request.getTaxId());
        supplier.setAddress(request.getAddress());
        supplier.setPaymentTerms(request.getPaymentTerms());
        supplier.setBankName(request.getBankName());
        supplier.setBankAccount(request.getBankAccount());
        supplier.setNote(request.getNote());

        Supplier saved = supplierRepository.save(supplier);
        log.info("供應商更新成功, id: {}, code: {}", saved.getId(), saved.getCode());

        return toDTO(saved);
    }

    /**
     * 取得供應商詳情
     */
    @Transactional(readOnly = true)
    public SupplierDTO getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("供應商不存在, id: " + id));
        return toDTO(supplier);
    }

    /**
     * 查詢供應商列表
     */
    @Transactional(readOnly = true)
    public PageResponse<SupplierDTO> listSuppliers(PageableRequest pageableRequest, String keyword, Boolean isActive) {
        Page<Supplier> supplierPage = supplierRepository.findByKeywordAndIsActive(
                keyword,
                isActive,
                pageableRequest.toPageable()
        );

        Page<SupplierDTO> dtoPage = supplierPage.map(this::toDTO);
        return PageResponse.from(dtoPage);
    }

    /**
     * 刪除供應商
     */
    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("供應商不存在, id: " + id));

        supplierRepository.delete(supplier);
        log.info("供應商刪除成功, id: {}, code: {}", id, supplier.getCode());
    }

    /**
     * 啟用供應商
     */
    @Transactional
    public SupplierDTO activateSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("供應商不存在, id: " + id));

        supplier.setIsActive(true);
        Supplier saved = supplierRepository.save(supplier);
        log.info("供應商啟用成功, id: {}, code: {}", id, supplier.getCode());

        return toDTO(saved);
    }

    /**
     * 停用供應商
     */
    @Transactional
    public SupplierDTO deactivateSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("供應商不存在, id: " + id));

        supplier.setIsActive(false);
        Supplier saved = supplierRepository.save(supplier);
        log.info("供應商停用成功, id: {}, code: {}", id, supplier.getCode());

        return toDTO(saved);
    }

    /**
     * Entity 轉換為 DTO
     */
    private SupplierDTO toDTO(Supplier supplier) {
        return SupplierDTO.builder()
                .id(supplier.getId())
                .code(supplier.getCode())
                .name(supplier.getName())
                .shortName(supplier.getShortName())
                .contactPerson(supplier.getContactPerson())
                .phone(supplier.getPhone())
                .mobile(supplier.getMobile())
                .fax(supplier.getFax())
                .email(supplier.getEmail())
                .taxId(supplier.getTaxId())
                .address(supplier.getAddress())
                .paymentTerms(supplier.getPaymentTerms())
                .paymentTermsDisplayName(PaymentTerms.getDisplayName(supplier.getPaymentTerms()))
                .bankName(supplier.getBankName())
                .bankAccount(supplier.getBankAccount())
                .isActive(supplier.getIsActive())
                .note(supplier.getNote())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}
