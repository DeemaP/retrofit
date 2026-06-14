package com.adas.retrofit.dto;

import com.adas.retrofit.entity.DamagePhoto;

import java.time.Instant;
import java.util.UUID;

/** Метаданные фото повреждения + ссылка на байты (проксируются через приложение). */
public record PhotoView(
        UUID id,
        String originalFilename,
        String contentType,
        long size,
        Instant uploadedAt,
        String url
) {

    public static PhotoView of(DamagePhoto photo) {
        return new PhotoView(
                photo.getId(),
                photo.getOriginalFilename(),
                photo.getContentType(),
                photo.getSize(),
                photo.getUploadedAt(),
                "/api/v1/orders/" + photo.getOrder().getId() + "/photos/" + photo.getId() + "/content");
    }
}