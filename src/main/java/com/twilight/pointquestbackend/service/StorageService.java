package com.twilight.pointquestbackend.service;

import java.io.InputStream;
import java.util.List;

/**
 * Abstraction for file/object storage.
 */
public interface StorageService {

    /**
     * Store an object and return its object key (relative path).
     *
     * @param objectKey   desired object key, e.g. folder/filename.ext
     * @param inputStream file stream
     * @param size        content length in bytes
     * @param contentType MIME type
     * @return stored object key
     */
    String store(String objectKey, InputStream inputStream, long size, String contentType);

    /**
     * Build a publicly accessible URL for the stored object.
     *
     * @param objectKey object key returned by {@link #store(String, InputStream, long, String)}
     * @return URL string
     */
    String getPublicUrl(String objectKey);

    /**
     * Build a temporary signed URL for the stored object.
     *
     * @param objectKey object key returned by {@link #store(String, InputStream, long, String)}
     * @param expirySeconds expiry time in seconds
     * @return signed URL string
     */
    String getSignedUrl(String objectKey, int expirySeconds);

    /**
     * 获取某前缀下的所有key
     */
    List<String> listKeys(String prefix);
}
