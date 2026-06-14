package com.adas.retrofit.dto;

/**
 * Тело формы «Проверка возможности дооснащения»: можно/нельзя, причина (если нельзя)
 * и id завершаемой задачи. Значение {@code possible} уходит в переменную процесса
 * {@code retrofitPossible} и управляет гейтвеем.
 */
public record FeasibilityRequest(
        boolean possible,
        String comment,
        String taskId
) {
}