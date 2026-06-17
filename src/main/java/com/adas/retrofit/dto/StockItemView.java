package com.adas.retrofit.dto;

import com.adas.retrofit.entity.StockItem;
import com.adas.retrofit.entity.StockType;

import java.util.UUID;

/** Складская позиция: остаток на складе по ключу type + sku. */
public record StockItemView(UUID id, StockType type, String sku, String name, int quantityOnHand) {

    public static StockItemView of(StockItem s) {
        return new StockItemView(s.getId(), s.getType(), s.getSku(), s.getName(), s.getQuantityOnHand());
    }
}