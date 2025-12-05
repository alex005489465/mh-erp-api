package com.morningharvest.erp.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePurchaseRequest {

    @NotNull(message = "進貨單ID不可為空")
    private Long id;

    @NotNull(message = "供應商ID不可為空")
    private Long supplierId;

    private LocalDate purchaseDate;

    @Size(max = 500, message = "備註不可超過 500 字元")
    private String note;

    @Valid
    private List<CreatePurchaseItemRequest> items;
}
