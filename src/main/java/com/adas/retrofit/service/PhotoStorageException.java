package com.adas.retrofit.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Ошибка работы с объектным хранилищем фотофиксации (MinIO). */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class PhotoStorageException extends RuntimeException {

    public PhotoStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}