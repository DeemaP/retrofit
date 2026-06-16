package com.adas.retrofit.dto;

import com.adas.retrofit.entity.CalibrationType;

/**
 * Тело формы «Кодирование и калибровка» (Activity_0xl0n6f). Сохраняет запись
 * программирования ЭБУ и запись калибровки, а флаг {@code systemHealthy} уходит
 * в одноимённую переменную процесса и управляет гейтвеем (исправна → тест-драйв,
 * неисправна → выявление причины).
 */
public record CodingRequest(
        String ecuName,
        String swBefore,
        String swAfter,
        String activatedFeatures,
        CalibrationType calibrationType,
        String calibrationParameters,
        boolean calibrationPassed,
        boolean systemHealthy,
        String taskId
) {
}