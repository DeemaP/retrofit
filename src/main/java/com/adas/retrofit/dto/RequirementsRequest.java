package com.adas.retrofit.dto;

/** Тело формы «Фиксация ТЗ»: пожелания/жалобы клиента + id завершаемой задачи. */
public record RequirementsRequest(
        String clientRequirements,
        String taskId
) {
}