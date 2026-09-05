package com.voyage.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default storage for dev/test: writes files under a local directory and returns
 * a URL under a configurable base path. Production uses the S3 adapter instead.
 */
@Component
@ConditionalOnProperty(name = "voyage.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final Path directory;
    private final String baseUrl;

    public LocalFileStorage(
            @Value("${voyage.storage.local.dir:build/uploads}") String dir,
            @Value("${voyage.storage.local.base-url:/files}") String baseUrl) {
        this.directory = Path.of(dir);
        this.baseUrl = baseUrl;
    }

    @Override
    public StoredFile store(byte[] content, String contentType, String originalFilename) {
        String key = StorageKeys.newKey("receipts", contentType);
        try {
            Path target = directory.resolve(key);
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file", e);
        }
        return new StoredFile(key, baseUrl + "/" + key);
    }
}
