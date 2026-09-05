package com.voyage.storage;

/**
 * Port for storing uploaded files (e.g. receipts). Implementations: local disk
 * (default, for dev/test) and S3 (activated by {@code voyage.storage.type=s3}).
 */
public interface FileStorage {

    StoredFile store(byte[] content, String contentType, String originalFilename);

    record StoredFile(String key, String url) {
    }
}
