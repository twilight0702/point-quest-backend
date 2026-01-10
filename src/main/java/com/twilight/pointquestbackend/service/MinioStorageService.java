package com.twilight.pointquestbackend.service;

import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    public MinioStorageService(MinioClient minioClient, StorageProperties storageProperties) {
        this.minioClient = minioClient;
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    public void ensureBucket() {
        String bucket = storageProperties.getMinio().getBucket();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new ServiceException(500, "storage_bucket_init_failed");
        }
    }

    @Override
    public String store(String objectKey, InputStream inputStream, long size, String contentType) {
        return store(objectKey, inputStream, size, contentType, Map.of());
    }

    @Override
    public String store(String objectKey, InputStream inputStream, long size, String contentType, Map<String, String> metadata) {
        String bucket = storageProperties.getMinio().getBucket();
        try {
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(inputStream, size, -1);
            if (StringUtils.hasText(contentType)) {
                builder.contentType(contentType);
            }
            if (metadata != null && !metadata.isEmpty()) {
                builder.userMetadata(metadata);
            }
            minioClient.putObject(builder.build());
            return objectKey;
        } catch (Exception e) {
            throw new ServiceException(500, "storage_upload_failed");
        }
    }

    @Override
    public String getPublicUrl(String objectKey) {
        StorageProperties.Minio minio = storageProperties.getMinio();
        String base = StringUtils.hasText(minio.getPublicUrl()) ? minio.getPublicUrl() : minio.getEndpoint();
        if (!StringUtils.hasText(base)) {
            throw new ServiceException(500, "storage_public_url_not_configured");
        }
        return appendPath(base, "/" + minio.getBucket() + "/" + objectKey);
    }

    @Override
    public String getSignedUrl(String objectKey, int expirySeconds) {
        log.info("minio 配置：public-url={}", storageProperties.getMinio().getPublicUrl());
        String bucket = storageProperties.getMinio().getBucket();
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(expirySeconds)
                            .build()
            );
        } catch (Exception e) {
            throw new ServiceException(500, "storage_signed_url_failed");
        }
    }

    @Override
    public void delete(String objectKey) {
        String bucket = storageProperties.getMinio().getBucket();
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new ServiceException(500, "storage_delete_failed");
        }
    }

    @Override
    public List<String> listKeys(String prefix) {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(storageProperties.getMinio().getBucket())
                        .prefix(prefix)
                        .recursive(true)
                        .build()
        );

        return StreamSupport.stream(results.spliterator(), false)
                .map(result -> {
                    try {
                        return result.get().objectName();
                    } catch (Exception e) {
                        throw new ServiceException(
                                500,
                                "storage_list_failed",
                                e
                        );
                    }
                }).toList();

    }

    private String appendPath(String base, String path) {
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }
}
