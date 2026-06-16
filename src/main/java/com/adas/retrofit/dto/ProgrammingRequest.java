package com.adas.retrofit.dto;

/**
 * Тело формы «Возврат авто в изначальное состояние / программирование» (Activity_08k31yn).
 * Сохраняет запись программирования ЭБУ с направлением RESTORE (восстановление исходного ПО).
 */
public record ProgrammingRequest(
        String ecuName,
        String swBefore,
        String swAfter,
        String activatedFeatures,
        String taskId
) {
}