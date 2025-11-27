package com.morningharvest.erp.option.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionTemplateGroupDetailDTO {

    private Long id;
    private String name;
    private Integer minSelections;
    private Integer maxSelections;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OptionTemplateValueDTO> values;
}
