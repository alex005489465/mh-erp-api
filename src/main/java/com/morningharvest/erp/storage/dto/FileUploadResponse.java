package com.morningharvest.erp.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 檔案上傳回應 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "檔案上傳回應")
public class FileUploadResponse {

    @Schema(description = "檔案 URL", example = "http://morning-harvest-minio:9000/public/products/abc123.jpg")
    private String fileUrl;

    @Schema(description = "原始檔案名稱", example = "product-image.jpg")
    private String originalFilename;

    @Schema(description = "檔案大小（位元組）", example = "102400")
    private Long fileSize;

    @Schema(description = "檔案類型", example = "image/jpeg")
    private String contentType;
}
