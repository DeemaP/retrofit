package com.adas.retrofit.dto;

/**
 * Пункт чек-листа слесарно-монтажных работ: заголовок этапа и признак выполнения.
 */
public record ChecklistItemDto(
        String title,
        boolean done
) {
}