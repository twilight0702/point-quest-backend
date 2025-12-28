package com.twilight.pointquestbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Storage configuration with MinIO defaults.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * Provider switch, currently only "minio" is supported.
     */
    private String provider = "minio";

    private final Minio minio = new Minio();

    @Getter
    @Setter
    public static class Minio {
        /**
         * Endpoint such as http://127.0.0.1:9000.
         */
        private String endpoint;
        /**
         * Access key (AK).
         */
        private String accessKey;
        /**
         * Secret key (SK).
         */
        private String secretKey;
        /**
         * Default bucket for uploads.
         */
        private String bucket;
        /**
         * Optional region, can stay null for most local setups.
         */
        private String region;
        /**
         * Optional public URL base (e.g. https://cdn.example.com). If empty, falls back to endpoint.
         */
        private String publicUrl;
        /**
         * Whether to use HTTPS when building pre-signed URLs.
         */
        private boolean useSsl = false;
    }
}
