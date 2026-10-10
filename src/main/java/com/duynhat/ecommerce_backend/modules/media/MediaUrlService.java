package com.duynhat.ecommerce_backend.modules.media;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class MediaUrlService {

    private final String backendUrl;

    public MediaUrlService(
            @Value("${app.backend-url}") String backendUrl
    ) {
        this.backendUrl = removeTrailingSlash(backendUrl);
    }

    public String toPublicUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        String encodedKey = URLEncoder.encode(
                objectKey,
                StandardCharsets.UTF_8
        );

        return backendUrl + "/api/media?key=" + encodedKey;
    }

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }
}