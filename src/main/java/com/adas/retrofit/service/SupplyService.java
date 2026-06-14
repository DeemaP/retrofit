package com.adas.retrofit.service;

import com.adas.retrofit.entity.Equipment;
import com.adas.retrofit.entity.Order;
import com.adas.retrofit.entity.Part;
import com.adas.retrofit.entity.SupplyOrderStatus;
import com.adas.retrofit.repository.EquipmentRepository;
import com.adas.retrofit.repository.OrderRepository;
import com.adas.retrofit.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Снабжение заявки на уровне <b>конкретной заявки</b>: проверка наличия на складе, заказ
 * недостающего (статусы) и выдача исполнителям (списание). Формирование справочных списков
 * по модели/году/типу — в {@link SpecCatalogService}.
 */
@Service
public class SupplyService {

    private final OrderRepository orderRepository;
    private final PartRepository partRepository;
    private final EquipmentRepository equipmentRepository;

    public SupplyService(OrderRepository orderRepository,
                         PartRepository partRepository,
                         EquipmentRepository equipmentRepository) {
        this.orderRepository = orderRepository;
        this.partRepository = partRepository;
        this.equipmentRepository = equipmentRepository;
    }

    // --- Проверка наличия на складе (альфа: заглушка «всё доступно», с демо-override недостачи) ---

    /**
     * Отмечает наличие запчастей на складе. По умолчанию всё доступно; если в процессе явно
     * выставлена переменная-override {@code partsShortage=true} — помечает позиции как отсутствующие
     * (для демонстрации ветки заказа).
     */
    @Transactional
    public boolean checkPartsStock(UUID orderId, boolean shortage) {
        List<Part> parts = partRepository.findByOrderId(orderId);
        parts.forEach(p -> p.setInStock(!shortage));
        partRepository.saveAll(parts);
        return !shortage && parts.stream().allMatch(Part::isInStock);
    }

    @Transactional
    public boolean checkEquipmentStock(UUID orderId, boolean shortage) {
        List<Equipment> equipment = equipmentRepository.findByOrderId(orderId);
        equipment.forEach(e -> e.setInStock(!shortage));
        equipmentRepository.saveAll(equipment);
        return !shortage && equipment.stream().allMatch(Equipment::isInStock);
    }

    @Transactional(readOnly = true)
    public List<Part> partsOf(UUID orderId) {
        return partRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public List<Equipment> equipmentOf(UUID orderId) {
        return equipmentRepository.findByOrderId(orderId);
    }

    // --- Заказ недостающего (ЮТ «Заказ недостающих запчастей и оборудования») ---

    @Transactional(readOnly = true)
    public List<Part> missingParts(UUID orderId) {
        return partRepository.findByOrderId(orderId).stream()
                .filter(p -> !p.isInStock())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Equipment> missingEquipment(UUID orderId) {
        return equipmentRepository.findByOrderId(orderId).stream()
                .filter(e -> !e.isInStock())
                .toList();
    }

    @Transactional(readOnly = true)
    public SupplyOrderStatus orderStatus(UUID orderId) {
        SupplyOrderStatus status = requireOrder(orderId).getSupplyOrderStatus();
        return status != null ? status : SupplyOrderStatus.TO_ORDER;
    }

    /**
     * Продвигает статус заказа. При переходе в {@code RECEIVED} ранее отсутствовавшие позиции
     * считаются поступившими на склад (inStock=true).
     */
    @Transactional
    public Order advanceOrderStatus(UUID orderId, SupplyOrderStatus target) {
        Order order = requireOrder(orderId);
        order.setSupplyOrderStatus(target);
        if (target == SupplyOrderStatus.RECEIVED) {
            List<Part> parts = partRepository.findByOrderId(orderId);
            parts.stream().filter(p -> !p.isInStock()).forEach(p -> p.setInStock(true));
            partRepository.saveAll(parts);
            List<Equipment> equipment = equipmentRepository.findByOrderId(orderId);
            equipment.stream().filter(e -> !e.isInStock()).forEach(e -> e.setInStock(true));
            equipmentRepository.saveAll(equipment);
        }
        return orderRepository.save(order);
    }

    // --- Выдача исполнителям (ЮТ «Выдача запчастей и оборудования исполнителям») ---

    /** Помечает выбранные позиции выданными (списанными со склада). Пустой набор = выдать все. */
    @Transactional
    public void issue(UUID orderId, Collection<UUID> partIds, Collection<UUID> equipmentIds) {
        Set<UUID> pIds = partIds != null ? Set.copyOf(partIds) : Set.of();
        Set<UUID> eIds = equipmentIds != null ? Set.copyOf(equipmentIds) : Set.of();

        List<Part> parts = partRepository.findByOrderId(orderId);
        parts.stream()
                .filter(p -> pIds.isEmpty() || pIds.contains(p.getId()))
                .forEach(p -> p.setIssued(true));
        partRepository.saveAll(parts);

        List<Equipment> equipment = equipmentRepository.findByOrderId(orderId);
        equipment.stream()
                .filter(e -> eIds.isEmpty() || eIds.contains(e.getId()))
                .forEach(e -> e.setIssued(true));
        equipmentRepository.saveAll(equipment);
    }

    private Order requireOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}