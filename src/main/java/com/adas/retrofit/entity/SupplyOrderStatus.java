package com.adas.retrofit.entity;

/**
 * Статус заказа недостающих запчастей/оборудования (этапы ЮТ «Заказ недостающих…»).
 * Прогресс: нужно заказать → заказано, ждём поступления → всё поступило на склад.
 */
public enum SupplyOrderStatus {
    TO_ORDER,
    ORDERED,
    RECEIVED
}