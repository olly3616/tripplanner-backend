package com.voyage.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Stores files in a private S3 bucket. Activated by {@code voyage.storage.type=s3}.
 * The returned URL points at the object; reads should go through a presigned GET
 * (a follow-up) since the bucket is private.
 */
@Component
@ConditionalOnProperty(name = "voyage.storage.type", havingValue = "s3")
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3FileStorage(S3Client s3Client,
                         @Value("${voyage.storage.s3.bucket}") String bucket,
                         @Value("${voyage.storage.s3.public-base-url:}") String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public StoredFile store(byte[] content, String contentType, String originalFilename) {
        String key = StorageKeys.newKey("receipts", contentType);
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(content));
        String base = publicBaseUrl.isBlank() ? "s3://" + bucket : publicBaseUrl;
        return new StoredFile(key, base + "/" + key);
    }
}
