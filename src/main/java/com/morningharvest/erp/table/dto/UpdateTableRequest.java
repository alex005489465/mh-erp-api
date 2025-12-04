package com.morningharvest.erp.table.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTableRequest {

    @Size(max = 20, message = "桌號長度不可超過 20 字元")
    private String tableNumber;

    @Min(value = 1, message = "容納人數至少為 1")
    private Integer capacity;

    private Boolean isActive;

    @Size(max = 200, message = "備註長度不可超過 200 字元")
    private String note;
}
