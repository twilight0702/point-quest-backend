package com.twilight.pointquestbackend.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Wiring for pluggable storage. Currently supports MinIO.
 */
@Configuration
public class StorageConfig {

    @Bean
    public MinioClient minioClient(StorageProperties properties) {
        if (!"minio".equalsIgnoreCase(properties.getProvider())) {
            throw new IllegalStateException("Unsupported storage provider: " + properties.getProvider());
        }
        StorageProperties.Minio minio = properties.getMinio();
        require(minio.getEndpoint(), "storage.minio.endpoint");
        require(minio.getAccessKey(), "storage.minio.access-key");
        require(minio.getSecretKey(), "storage.minio.secret-key");
        require(minio.getBucket(), "storage.minio.bucket");

        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey());
        if (StringUtils.hasText(minio.getRegion())) {
            builder.region(minio.getRegion());
        }
        return builder.build();
    }

    private static void require(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing required config: " + name);
        }
    }
}
