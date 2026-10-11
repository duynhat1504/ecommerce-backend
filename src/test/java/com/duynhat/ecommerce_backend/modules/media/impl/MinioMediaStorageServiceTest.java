package com.duynhat.ecommerce_backend.modules.media.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.config.MinioProperties;
import com.duynhat.ecommerce_backend.modules.media.MediaObject;
import io.minio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MinioMediaStorageServiceTest {

    private MinioMediaStorageService mediaStorageService;

    @BeforeEach
    void setUp() {
        MinioClient minioClient = mock(MinioClient.class);

        MinioProperties properties = new MinioProperties();
        properties.setBucket("ecommerce-coffee-media");

        mediaStorageService = new MinioMediaStorageService(
                minioClient,
                properties
        );
    }

    @Test
    void upload_shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        assertThatThrownBy(() ->
                mediaStorageService.upload(
                        file,
                        "products/test-product"
                )
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("File is required");
    }

    @Test
    void upload_shouldRejectUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "test".getBytes()
        );

        assertThatThrownBy(() ->
                mediaStorageService.upload(
                        file,
                        "products/test-product"
                )
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only JPEG, PNG, and WebP images are allowed");
    }

    @Test
    void upload_shouldRejectFileLargerThanFiveMegabytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                new byte[5 * 1024 * 1024 + 1]
        );

        assertThatThrownBy(() ->
                mediaStorageService.upload(
                        file,
                        "products/test-product"
                )
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("File size must not exceed 5 MB");
    }

    @Test
    void upload_shouldUploadValidImageAndReturnObjectKey() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);

        MinioProperties properties = new MinioProperties();
        properties.setBucket("ecommerce-coffee-media");

        MinioMediaStorageService service =
                new MinioMediaStorageService(
                        minioClient,
                        properties
                );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "product.jpg",
                "image/jpeg",
                "image-content".getBytes()
        );

        String objectKey = service.upload(
                file,
                "products/test-product"
        );

        assertThat(objectKey)
                .startsWith("products/test-product/")
                .endsWith(".jpg");

        verify(minioClient).putObject(
                any(PutObjectArgs.class)
        );
    }

    @Test
    void delete_shouldRemoveObjectFromMinio() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);

        MinioProperties properties = new MinioProperties();
        properties.setBucket("ecommerce-coffee-media");

        MinioMediaStorageService service =
                new MinioMediaStorageService(
                        minioClient,
                        properties
                );

        String objectKey =
                "products/test-product/test-image.jpg";

        service.delete(objectKey);

        verify(minioClient).removeObject(
                any(RemoveObjectArgs.class)
        );
    }

    @Test
    void delete_shouldIgnoreBlankObjectKey() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);

        MinioProperties properties = new MinioProperties();
        properties.setBucket("ecommerce-coffee-media");

        MinioMediaStorageService service =
                new MinioMediaStorageService(
                        minioClient,
                        properties
                );

        service.delete("   ");

        verify(
                minioClient,
                org.mockito.Mockito.never()
        ).removeObject(
                any(RemoveObjectArgs.class)
        );
    }

    @Test
    void get_shouldReturnObjectContentAndContentType() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);

        MinioProperties properties = new MinioProperties();
        properties.setBucket("ecommerce-coffee-media");

        MinioMediaStorageService service =
                new MinioMediaStorageService(
                        minioClient,
                        properties
                );

        String objectKey =
                "products/test-product/test-image.jpg";

        byte[] content = "image-content".getBytes();

        StatObjectResponse statResponse =
                mock(StatObjectResponse.class);

        when(statResponse.contentType())
                .thenReturn("image/jpeg");

        GetObjectResponse getObjectResponse =
                new GetObjectResponse(
                        null,
                        "ecommerce-coffee-media",
                        null,
                        objectKey,
                        new ByteArrayInputStream(content)
                );

        when(minioClient.statObject(
                any(StatObjectArgs.class)
        )).thenReturn(statResponse);

        when(minioClient.getObject(
                any(GetObjectArgs.class)
        )).thenReturn(getObjectResponse);

        MediaObject result =
                service.get(objectKey);

        assertThat(result.content())
                .isEqualTo(content);

        assertThat(result.contentType())
                .isEqualTo("image/jpeg");
    }
}