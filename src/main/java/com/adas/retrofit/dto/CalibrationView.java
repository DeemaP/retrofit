package com.adas.retrofit.dto;

import com.adas.retrofit.entity.CalibrationRecord;
import com.adas.retrofit.entity.CalibrationType;

import java.time.Instant;
import java.util.UUID;

/** Запись о калибровке для отображения. */
public record CalibrationView(
        UUID id,
        CalibrationType type,
        String parameters,
        boolean passed,
        Instant createdAt
) {

    public static CalibrationView of(CalibrationRecord r) {
        return new CalibrationView(
                r.getId(), r.getType(), r.getParameters(), r.isPassed(), r.getCreatedAt());
    }
}