package com.morningharvest.erp.storage.controller;

import com.morningharvest.erp.common.dto.ApiResponse;
import com.morningharvest.erp.storage.dto.FileUploadResponse;
import com.morningharvest.erp.storage.service.StorageService;
import com.morningharvest.erp.storage.util.FileValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 檔案管理 Controller
 * 提供檔案上傳、刪除等 API
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "檔案管理", description = "檔案上傳、刪除等操作")
public class FileController {

    private final StorageService storageService;

    /**
     * 上傳檔案
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "上傳檔案",
            description = "上傳圖片檔案到 MinIO，支援 jpg, png, gif, webp, bmp 等格式，檔案大小限制 10MB"
    )
    public ApiResponse<FileUploadResponse> uploadFile(
            @Parameter(
                    description = "上傳的圖片檔案",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file,

            @Parameter(
                    description = "存放資料夾 (products, documents 等)",
                    example = "products"
            )
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        log.info("收到檔案上傳請求 - 檔案名稱: {}, 大小: {} bytes, 資料夾: {}",
                file.getOriginalFilename(), file.getSize(), folder);

        // 驗證檔案
        FileValidator.validateImageFile(file);

        // 上傳檔案
        String fileUrl = storageService.uploadFile(file, folder);

        // 建立回應
        FileUploadResponse response = new FileUploadResponse(
                fileUrl,
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType()
        );

        log.info("檔案上傳成功 - URL: {}", fileUrl);
        return ApiResponse.success(response);
    }

    /**
     * 刪除檔案
     */
    @PostMapping("/delete")
    @Operation(summary = "刪除檔案", description = "從 MinIO 刪除指定的檔案")
    public ApiResponse<Void> deleteFile(
            @Parameter(description = "檔案 URL", required = true, example = "http://morning-harvest-minio:9000/public/products/abc123.jpg")
            @RequestParam("fileUrl") String fileUrl
    ) {
        log.info("收到檔案刪除請求 - URL: {}", fileUrl);

        storageService.deleteFile(fileUrl);

        log.info("檔案刪除成功 - URL: {}", fileUrl);
        return ApiResponse.success("檔案刪除成功");
    }
}
