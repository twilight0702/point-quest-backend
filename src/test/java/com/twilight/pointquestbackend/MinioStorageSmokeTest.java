package com.twilight.pointquestbackend;

import com.twilight.pointquestbackend.config.StorageProperties;
import com.twilight.pointquestbackend.service.MinioStorageService;
import com.twilight.pointquestbackend.service.StorageService;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Manual smoke test for MinIO storage using application.yml defaults.
 */
@Slf4j
public class MinioStorageSmokeTest {

    @Test
    void uploadAndReadBack() throws Exception {
        StorageProperties props = loadPropsFromDefaults();
        // 打印配置项
        String format = String.format(
                "ac: %s, ck: %s, url: %s, bucket: %s",
                props.getMinio().getAccessKey(),
                props.getMinio().getSecretKey(),
                props.getMinio().getEndpoint(),
                props.getMinio().getBucket()
        );
        System.out.println(format);


        MinioClient client = MinioClient.builder()
                .endpoint(props.getMinio().getEndpoint())
                .credentials(props.getMinio().getAccessKey(), props.getMinio().getSecretKey())
                .build();
        StorageService storage = new MinioStorageService(client, props);

        // Ensure bucket exists for the smoke test
        boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(props.getMinio().getBucket()).build());
        if (!exists) {
            client.makeBucket(
                    MakeBucketArgs.builder().bucket(props.getMinio().getBucket()).build());
        }

        String content = "hello-minio-" + UUID.randomUUID();
        String objectKey = "test/" + UUID.randomUUID() + ".txt";
        try (InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            storage.store(objectKey, in, content.getBytes(StandardCharsets.UTF_8).length, "text/plain");
        }

        try (InputStream fetched = client.getObject(GetObjectArgs.builder()
                .bucket(props.getMinio().getBucket())
                .object(objectKey)
                .build())) {
            String readBack = new String(fetched.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(content, readBack);
        } finally {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.getMinio().getBucket())
                    .object(objectKey)
                    .build());
        }
    }

    private StorageProperties loadPropsFromDefaults() {
        StorageProperties properties = new StorageProperties();
        StorageProperties.Minio minio = properties.getMinio();
        minio.setEndpoint("http://localhost:9000");
        minio.setAccessKey("admin");
        minio.setSecretKey("admin123456");
        minio.setBucket("pointquest");
        minio.setRegion(null);
        minio.setPublicUrl(null);
        return properties;
    }
}
