package com.duynhat.ecommerce_backend.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioBucketInitializer implements ApplicationRunner {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String bucket = minioProperties.getBucket();

        log.info("Checking MinIO bucket: {}", bucket);

        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucket)
                        .build()
        );

        log.info("MinIO bucket {} exists: {}", bucket, exists);

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucket)
                            .build()
            );

            log.info("Created MinIO bucket: {}", bucket);
        }
    }
}