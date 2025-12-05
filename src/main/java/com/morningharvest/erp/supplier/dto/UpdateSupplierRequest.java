package com.morningharvest.erp.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新供應商請求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSupplierRequest {

    @NotNull(message = "供應商 ID 不可為空")
    private Long id;

    @NotBlank(message = "供應商編號不可為空")
    @Size(max = 20, message = "供應商編號不可超過 20 字元")
    private String code;

    @NotBlank(message = "供應商名稱不可為空")
    @Size(max = 100, message = "供應商名稱不可超過 100 字元")
    private String name;

    @Size(max = 50, message = "簡稱不可超過 50 字元")
    private String shortName;

    @Size(max = 50, message = "聯絡人不可超過 50 字元")
    private String contactPerson;

    @Size(max = 20, message = "電話不可超過 20 字元")
    private String phone;

    @Size(max = 20, message = "手機不可超過 20 字元")
    private String mobile;

    @Size(max = 20, message = "傳真不可超過 20 字元")
    private String fax;

    @Email(message = "電子郵件格式不正確")
    @Size(max = 100, message = "電子郵件不可超過 100 字元")
    private String email;

    @Size(max = 20, message = "統一編號不可超過 20 字元")
    private String taxId;

    @Size(max = 200, message = "地址不可超過 200 字元")
    private String address;

    @Size(max = 50, message = "付款條件不可超過 50 字元")
    private String paymentTerms;

    @Size(max = 100, message = "銀行名稱不可超過 100 字元")
    private String bankName;

    @Size(max = 50, message = "銀行帳號不可超過 50 字元")
    private String bankAccount;

    @Size(max = 500, message = "備註不可超過 500 字元")
    private String note;
}
