package com.adas.retrofit.dto;

import com.adas.retrofit.entity.Equipment;
import com.adas.retrofit.entity.Part;

import java.util.List;
import java.util.UUID;

/** Списки снабжения заявки: отдельно запчасти (в авто) и оборудование (оснастка цеха). */
public record SupplyResponse(
        List<PartView> parts,
        List<EquipmentView> equipment
) {

    public record PartView(UUID id, String name, String article, int quantity, boolean inStock, boolean issued) {
        public static PartView of(Part p) {
            return new PartView(p.getId(), p.getName(), p.getArticle(), p.getQuantity(), p.isInStock(), p.isIssued());
        }
    }

    public record EquipmentView(UUID id, String name, String article, boolean inStock, boolean issued) {
        public static EquipmentView of(Equipment e) {
            return new EquipmentView(e.getId(), e.getName(), e.getArticle(), e.isInStock(), e.isIssued());
        }
    }

    public static SupplyResponse of(List<Part> parts, List<Equipment> equipment) {
        return new SupplyResponse(
                parts.stream().map(PartView::of).toList(),
                equipment.stream().map(EquipmentView::of).toList());
    }
}
