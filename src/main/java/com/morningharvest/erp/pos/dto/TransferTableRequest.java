package com.morningharvest.erp.pos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferTableRequest {

    @NotNull(message = "來源桌位 ID 不可為空")
    private Long fromTableId;

    @NotNull(message = "目標桌位 ID 不可為空")
    private Long toTableId;
}
