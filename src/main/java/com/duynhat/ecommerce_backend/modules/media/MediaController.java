package com.duynhat.ecommerce_backend.modules.media;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaStorageService mediaStorageService;

    @GetMapping
    public ResponseEntity<byte[]> getMedia(
            @RequestParam String key
    ) {
        MediaObject media = mediaStorageService.get(key);

        MediaType mediaType = MediaType.parseMediaType(
                media.contentType() == null
                        ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                        : media.contentType()
        );

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(
                        CacheControl.maxAge(
                                7,
                                TimeUnit.DAYS
                        ).cachePublic()
                )
                .body(media.content());
    }
}