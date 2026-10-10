package com.duynhat.ecommerce_backend.modules.media;

public record MediaObject(
        byte[] content,
        String contentType
) {
}