package com.adas.retrofit.dto;

/**
 * Позиция списка снабжения (запчасть или оборудование). Используется и при отдаче
 * подсказки из каталога, и при сохранении заполненной формы. {@code quantity}
 * актуально для запчастей; для оборудования игнорируется (по умолчанию 1).
 */
public record SupplyItem(
        String name,
        String article,
        Integer quantity
) {
}