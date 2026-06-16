package com.adas.retrofit.dto;

import java.util.List;

/**
 * Тело формы чек-листа работ («Монтажные и слесарные работы» / «Демонтаж установленной системы»).
 * Фаза определяется эндпоинтом. {@code taskId} — завершаемая задача процесса.
 */
public record ChecklistRequest(
        List<ChecklistItemDto> items,
        String taskId
) {
}