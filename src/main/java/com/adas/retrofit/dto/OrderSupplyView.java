package com.adas.retrofit.dto;

import com.adas.retrofit.entity.Equipment;
import com.adas.retrofit.entity.Part;
import com.adas.retrofit.entity.SupplyOrderStatus;

import java.util.List;

/** Состояние заказа недостающего: текущий статус + списки отсутствующих запчастей/оборудования. */
public record OrderSupplyView(
        SupplyOrderStatus status,
        List<SupplyResponse.PartView> parts,
        List<SupplyResponse.EquipmentView> equipment
) {

    public static OrderSupplyView of(SupplyOrderStatus status, List<Part> parts, List<Equipment> equipment) {
        return new OrderSupplyView(
                status,
                parts.stream().map(SupplyResponse.PartView::of).toList(),
                equipment.stream().map(SupplyResponse.EquipmentView::of).toList());
    }
}