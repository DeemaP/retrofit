package com.adas.retrofit.dto;

import com.adas.retrofit.entity.ProgrammingDirection;
import com.adas.retrofit.entity.ProgrammingRecord;

import java.time.Instant;
import java.util.UUID;

/** Запись о программировании ЭБУ для отображения. */
public record ProgrammingView(
        UUID id,
        String ecuName,
        String swBefore,
        String swAfter,
        String activatedFeatures,
        ProgrammingDirection direction,
        Instant createdAt
) {

    public static ProgrammingView of(ProgrammingRecord r) {
        return new ProgrammingView(
                r.getId(), r.getEcuName(), r.getSwBefore(), r.getSwAfter(),
                r.getActivatedFeatures(), r.getDirection(), r.getCreatedAt());
    }
}