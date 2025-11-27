package com.morningharvest.erp.storage.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 檔案儲存服務介面
 * 定義檔案上傳、刪除、URL 生成等操作
 */
public interface StorageService {

    /**
     * 上傳檔案到儲存系統
     *
     * @param file 上傳的檔案
     * @param folder 存放資料夾（如 "products", "documents"）
     * @return 檔案的完整 URL
     */
    String uploadFile(MultipartFile file, String folder);

    /**
     * 刪除檔案
     *
     * @param fileUrl 檔案 URL
     */
    void deleteFile(String fileUrl);

    /**
     * 取得檔案的完整 URL
     *
     * @param bucket bucket 名稱
     * @param objectName 物件名稱（含路徑）
     * @return 檔案的完整 URL
     */
    String getFileUrl(String bucket, String objectName);
}
