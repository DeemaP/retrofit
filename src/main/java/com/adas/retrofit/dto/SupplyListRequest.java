package com.adas.retrofit.dto;

import java.util.List;

/**
 * Тело формы «Формирование списка запчастей/оборудования»: позиции списка и
 * (опционально) id user task, которую нужно завершить после сохранения.
 */
public record SupplyListRequest(
        List<SupplyItem> items,
        String taskId
) {
}