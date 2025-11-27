package com.morningharvest.erp.storage.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * MinIO 儲存服務實作
 * 處理檔案上傳到 MinIO 的所有邏輯
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;

    @Value("${storage.minio.endpoint}")
    private String endpoint;

    @Value("${storage.minio.public-url:${storage.minio.endpoint}}")
    private String publicUrl;

    /**
     * 上傳檔案到 MinIO
     *
     * @param file 上傳的檔案
     * @param folder 存放資料夾（如 "products", "documents"）
     * @return 檔案的完整 URL
     */
    @Override
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // 產生唯一檔名：UUID + 原始副檔名
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID() + extension;

            // 組合物件名稱：folder/filename
            String objectName = folder + "/" + uniqueFilename;

            // 上傳到 MinIO 的 public bucket
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket("public")
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            // 產生並回傳檔案 URL
            String fileUrl = getFileUrl("public", objectName);
            log.info("檔案上傳成功 - 原始檔名: {}, URL: {}", originalFilename, fileUrl);

            return fileUrl;

        } catch (Exception e) {
            log.error("檔案上傳失敗 - {}", file.getOriginalFilename(), e);
            throw new RuntimeException("檔案上傳失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 刪除檔案
     *
     * @param fileUrl 檔案 URL
     */
    @Override
    public void deleteFile(String fileUrl) {
        try {
            // 從 URL 解析 bucket 和 objectName
            // URL 格式: http://morning-harvest-minio:9000/public/products/xxx.jpg
            String[] parts = fileUrl.replace(endpoint + "/", "").split("/", 2);
            if (parts.length < 2) {
                throw new IllegalArgumentException("無效的檔案 URL: " + fileUrl);
            }

            String bucket = parts[0];
            String objectName = parts[1];

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );

            log.info("檔案刪除成功 - URL: {}", fileUrl);

        } catch (Exception e) {
            log.error("檔案刪除失敗 - URL: {}", fileUrl, e);
            throw new RuntimeException("檔案刪除失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 取得檔案的完整 URL
     *
     * @param bucket bucket 名稱
     * @param objectName 物件名稱（含路徑）
     * @return 檔案的完整 URL（對外訪問 URL）
     */
    @Override
    public String getFileUrl(String bucket, String objectName) {
        // 使用 public-url (CDN URL) 而非內部 endpoint
        // URL 格式範例: https://cdn.example.com/bucket/objectName
        return publicUrl + "/" + bucket + "/" + objectName;
    }

    /**
     * 取得檔案副檔名（含點號）
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
