package com.duynhat.ecommerce_backend.modules.media.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.config.MinioProperties;
import com.duynhat.ecommerce_backend.modules.media.MediaStorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioMediaStorageService implements MediaStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public String upload(
            MultipartFile file,
            String prefix
    ) {
        validateFile(file);

        String objectKey = buildObjectKey(
                prefix,
                file.getOriginalFilename()
        );

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .stream(
                                    file.getInputStream(),
                                    file.getSize(),
                                    -1
                            )
                            .contentType(file.getContentType())
                            .build()
            );

            return objectKey;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to upload file to MinIO",
                    ex
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
    }

    private String buildObjectKey(
            String prefix,
            String originalFilename
    ) {
        String extension = getExtension(originalFilename);

        return "%s/%s%s".formatted(
                normalizePrefix(prefix),
                UUID.randomUUID(),
                extension
        );
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new BadRequestException("File prefix is required");
        }

        return prefix
                .trim()
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }

    private String getExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }

        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex < 0) {
            return "";
        }

        return filename
                .substring(dotIndex)
                .toLowerCase();
    }
}