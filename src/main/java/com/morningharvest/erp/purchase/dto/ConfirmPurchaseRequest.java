package com.morningharvest.erp.purchase.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPurchaseRequest {

    @NotNull(message = "進貨單ID不可為空")
    private Long id;

    @Size(max = 50, message = "確認人員不可超過 50 字元")
    private String confirmedBy;
}
