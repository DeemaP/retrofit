package com.adas.retrofit.dto;

import com.adas.retrofit.entity.Equipment;
import com.adas.retrofit.entity.Part;

import java.util.List;
import java.util.UUID;
import java.util.function.ToIntFunction;

/** Списки снабжения заявки: отдельно запчасти (в авто) и оборудование (оснастка цеха). */
public record SupplyResponse(
        List<PartView> parts,
        List<EquipmentView> equipment
) {

    public record PartView(UUID id, String name, String article, int quantity,
                           boolean inStock, boolean ordered, boolean issued, Integer stockOnHand) {
        public static PartView of(Part p, Integer stockOnHand) {
            return new PartView(p.getId(), p.getName(), p.getArticle(), p.getQuantity(),
                    p.isInStock(), p.isOrdered(), p.isIssued(), stockOnHand);
        }
    }

    public record EquipmentView(UUID id, String name, String article,
                                boolean inStock, boolean ordered, boolean issued) {
        public static EquipmentView of(Equipment e) {
            return new EquipmentView(e.getId(), e.getName(), e.getArticle(),
                    e.isInStock(), e.isOrdered(), e.isIssued());
        }
    }

    public static SupplyResponse of(List<Part> parts, List<Equipment> equipment,
                                    ToIntFunction<Part> partStock) {
        return new SupplyResponse(
                parts.stream().map(p -> PartView.of(p, partStock.applyAsInt(p))).toList(),
                equipment.stream().map(EquipmentView::of).toList());
    }
}