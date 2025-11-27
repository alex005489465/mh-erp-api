package com.morningharvest.erp.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 配置類
 * 建立 MinioClient Bean 並初始化必要的 bucket
 */
@Slf4j
@Configuration
public class MinioConfig {

    @Value("${storage.minio.endpoint}")
    private String endpoint;

    @Value("${storage.minio.access-key}")
    private String accessKey;

    @Value("${storage.minio.secret-key}")
    private String secretKey;

    /**
     * 建立 MinioClient Bean
     */
    @Bean
    public MinioClient minioClient() {
        log.info("正在初始化 MinIO Client - endpoint: {}", endpoint);

        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            // 初始化必要的 bucket
            initializeBuckets(client);

            log.info("MinIO Client 初始化成功");
            return client;
        } catch (Exception e) {
            log.error("MinIO Client 初始化失敗", e);
            throw new RuntimeException("無法連線到 MinIO 伺服器: " + e.getMessage(), e);
        }
    }

    /**
     * 初始化 bucket
     * 建立 public 和 private 兩個 bucket
     */
    private void initializeBuckets(MinioClient client) {
        try {
            // 建立 public bucket (用於公開存取的檔案，如商品圖片)
            createBucketIfNotExists(client, "public", true);

            // 建立 private bucket (用於私密檔案)
            createBucketIfNotExists(client, "private", false);

        } catch (Exception e) {
            log.error("初始化 bucket 失敗", e);
            throw new RuntimeException("初始化 MinIO bucket 失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 建立 bucket (如果不存在)
     */
    private void createBucketIfNotExists(MinioClient client, String bucketName, boolean isPublic) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());

        if (!exists) {
            log.info("建立 bucket: {}", bucketName);
            client.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());

            // 如果是 public bucket，設定公開讀取政策
            if (isPublic) {
                setPublicPolicy(client, bucketName);
            }

            log.info("Bucket {} 建立成功", bucketName);
        } else {
            log.info("Bucket {} 已存在", bucketName);
        }
    }

    /**
     * 設定 bucket 為公開讀取
     */
    private void setPublicPolicy(MinioClient client, String bucketName) throws Exception {
        // MinIO 的公開讀取政策 JSON
        String policy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": {"AWS": "*"},
                            "Action": ["s3:GetObject"],
                            "Resource": ["arn:aws:s3:::%s/*"]
                        }
                    ]
                }
                """.formatted(bucketName);

        client.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(bucketName)
                .config(policy)
                .build());

        log.info("Bucket {} 已設定為公開讀取", bucketName);
    }
}
