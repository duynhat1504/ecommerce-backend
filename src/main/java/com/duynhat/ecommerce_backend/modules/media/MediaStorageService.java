package com.duynhat.ecommerce_backend.modules.media;

import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {

    String upload(
            MultipartFile file,
            String prefix
    );
}