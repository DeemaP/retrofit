package com.adas.retrofit.dto;

import com.adas.retrofit.entity.StockType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Установка остатка позиции на складе (демо/корректировки). */
public record SetStockRequest(
        @NotNull StockType type,
        @NotBlank String sku,
        String name,
        @PositiveOrZero int quantityOnHand
) {
}