package com.morningharvest.erp.pos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EndDiningRequest {

    @NotNull(message = "桌位 ID 不可為空")
    private Long tableId;
}
