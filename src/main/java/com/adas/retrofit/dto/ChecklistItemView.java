package com.adas.retrofit.dto;

import com.adas.retrofit.entity.WorkChecklistItem;

/** Пункт чек-листа работ для отображения. */
public record ChecklistItemView(
        String title,
        boolean done
) {

    public static ChecklistItemView of(WorkChecklistItem item) {
        return new ChecklistItemView(item.getTitle(), item.isDone());
    }
}