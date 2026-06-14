package com.adas.retrofit.dto;

import java.util.Date;

/** Представление активной user task процесса. */
public record TaskView(
        String id,
        String name,
        String assignee,
        String candidateGroups,
        Date created,
        /** Ключ задачи в BPMN (Activity_xxx) — по нему фронтенд выбирает форму. */
        String taskDefinitionKey
) {
}