package com.voyage.storage;

import java.util.Map;
import java.util.UUID;

/** Shared helpers for building storage object keys. */
final class StorageKeys {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private StorageKeys() {
    }

    static String newKey(String prefix, String contentType) {
        return prefix + "/" + UUID.randomUUID().toString().replace("-", "")
                + EXTENSIONS.getOrDefault(contentType, "");
    }
}
