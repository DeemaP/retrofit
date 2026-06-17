package com.adas.retrofit.dto;

import com.adas.retrofit.entity.Equipment;
import com.adas.retrofit.entity.Part;
import com.adas.retrofit.entity.SupplyOrderStatus;

import java.util.List;
import java.util.function.ToIntFunction;

/** Состояние заказа недостающего: сводный статус + списки отсутствующих запчастей/оборудования. */
public record OrderSupplyView(
        SupplyOrderStatus status,
        List<SupplyResponse.PartView> parts,
        List<SupplyResponse.EquipmentView> equipment
) {

    public static OrderSupplyView of(SupplyOrderStatus status, List<Part> parts, List<Equipment> equipment,
                                     ToIntFunction<Part> partStock) {
        return new OrderSupplyView(
                status,
                parts.stream().map(p -> SupplyResponse.PartView.of(p, partStock.applyAsInt(p))).toList(),
                equipment.stream().map(SupplyResponse.EquipmentView::of).toList());
    }
}