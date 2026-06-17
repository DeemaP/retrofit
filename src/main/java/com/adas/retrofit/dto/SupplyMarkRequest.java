package com.adas.retrofit.dto;

import java.util.List;
import java.util.UUID;

/**
 * По-позиционная отметка в ЮТ «Заказ недостающих…»: id отмечаемых запчастей/оборудования
 * (пустые списки = все недостающие). {@code taskId} — для завершения задачи, когда всё поступило.
 */
public record SupplyMarkRequest(
        List<UUID> partIds,
        List<UUID> equipmentIds,
        String taskId
) {
}