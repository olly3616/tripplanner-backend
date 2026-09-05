package com.voyage.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** Provides the S3 client only when S3 storage is enabled. */
@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "voyage.storage.type", havingValue = "s3")
    public S3Client s3Client(@Value("${voyage.storage.s3.region:ap-northeast-2}") String region) {
        // Credentials come from the default provider chain (env vars, instance profile, etc.).
        return S3Client.builder().region(Region.of(region)).build();
    }
}
