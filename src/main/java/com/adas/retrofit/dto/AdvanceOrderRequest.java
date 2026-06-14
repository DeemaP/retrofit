package com.adas.retrofit.dto;

import com.adas.retrofit.entity.SupplyOrderStatus;

/**
 * Продвижение статуса заказа недостающего. Если {@code status == RECEIVED} и передан {@code taskId},
 * ЮТ «Заказ недостающих…» завершается.
 */
public record AdvanceOrderRequest(
        SupplyOrderStatus status,
        String taskId
) {
}