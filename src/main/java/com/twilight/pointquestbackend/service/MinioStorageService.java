package com.twilight.pointquestbackend.service;

import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.io.InputStream;

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
        String bucket = storageProperties.getMinio().getBucket();
        try {
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(inputStream, size, -1);
            if (StringUtils.hasText(contentType)) {
                builder.contentType(contentType);
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

    private String appendPath(String base, String path) {
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }
}
