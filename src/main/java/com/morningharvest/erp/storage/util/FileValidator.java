package com.morningharvest.erp.storage.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 檔案驗證工具類
 * 驗證檔案類型、大小等
 */
public class FileValidator {

    // 允許的圖片格式
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

    // 允許的圖片 MIME 類型
    private static final List<String> ALLOWED_IMAGE_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    // 最大檔案大小 (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 驗證是否為有效的圖片檔案
     *
     * @param file 上傳的檔案
     * @throws IllegalArgumentException 如果檔案無效
     */
    public static void validateImageFile(MultipartFile file) {
        // 檢查檔案是否為空
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("檔案不能為空");
        }

        // 檢查檔案大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("檔案大小不能超過 %d MB", MAX_FILE_SIZE / 1024 / 1024)
            );
        }

        // 檢查檔案名稱
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("檔案名稱不能為空");
        }

        // 檢查副檔名
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "不支援的圖片格式。允許的格式：" + String.join(", ", ALLOWED_IMAGE_EXTENSIONS)
            );
        }

        // 檢查 MIME 類型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "不支援的檔案類型。允許的類型：" + String.join(", ", ALLOWED_IMAGE_MIME_TYPES)
            );
        }
    }

    /**
     * 清理檔案名稱，移除潛在的危險字元
     *
     * @param filename 原始檔案名稱
     * @return 清理後的檔案名稱
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return "";
        }

        // 移除路徑穿透字元
        String sanitized = filename.replaceAll("[./\\\\]", "_");

        // 只保留檔名（移除路徑）
        int lastSeparator = Math.max(
                sanitized.lastIndexOf('/'),
                sanitized.lastIndexOf('\\')
        );
        if (lastSeparator != -1) {
            sanitized = sanitized.substring(lastSeparator + 1);
        }

        return sanitized;
    }

    /**
     * 取得檔案副檔名（含點號）
     */
    private static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
