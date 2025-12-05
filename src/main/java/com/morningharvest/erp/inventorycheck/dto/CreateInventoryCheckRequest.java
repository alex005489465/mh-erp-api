package com.morningharvest.erp.inventorycheck.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInventoryCheckRequest {

    private LocalDate checkDate;

    @Size(max = 500, message = "備註不可超過 500 字元")
    private String note;
}
