package com.adas.retrofit.service;

import com.adas.retrofit.entity.Equipment;
import com.adas.retrofit.entity.Order;
import com.adas.retrofit.entity.Part;
import com.adas.retrofit.entity.StockItem;
import com.adas.retrofit.entity.StockType;
import com.adas.retrofit.entity.SupplyOrderStatus;
import com.adas.retrofit.repository.EquipmentRepository;
import com.adas.retrofit.repository.OrderRepository;
import com.adas.retrofit.repository.PartRepository;
import com.adas.retrofit.repository.StockItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Снабжение заявки: проверка наличия на складе по <b>количеству</b>, заказ недостающего
 * (по-позиционные этапы: требуется заказать → заказано → поступило) и выдача исполнителям
 * (списание остатка). Остатки склада ведутся в {@link StockItem}; формирование справочных
 * списков по модели/году/типу — в {@link SpecCatalogService}.
 */
@Service
public class SupplyService {

    private final OrderRepository orderRepository;
    private final PartRepository partRepository;
    private final EquipmentRepository equipmentRepository;
    private final StockItemRepository stockRepository;

    public SupplyService(OrderRepository orderRepository,
                         PartRepository partRepository,
                         EquipmentRepository equipmentRepository,
                         StockItemRepository stockRepository) {
        this.orderRepository = orderRepository;
        this.partRepository = partRepository;
        this.equipmentRepository = equipmentRepository;
        this.stockRepository = stockRepository;
    }

    // --- Проверка наличия на складе по количеству ---

    /**
     * Помечает запчасти доступными, если на складе хватает количества под потребность.
     * Демо-override {@code shortage=true} принудительно делает все позиции недостающими.
     */
    @Transactional
    public boolean checkPartsStock(UUID orderId, boolean shortage) {
        List<Part> parts = partRepository.findByOrderId(orderId);
        parts.forEach(p -> p.setInStock(!shortage && onHand(StockType.PART, sku(p.getArticle(), p.getName())) >= p.getQuantity()));
        partRepository.saveAll(parts);
        return !shortage && parts.stream().allMatch(Part::isInStock);
    }

    @Transactional
    public boolean checkEquipmentStock(UUID orderId, boolean shortage) {
        List<Equipment> equipment = equipmentRepository.findByOrderId(orderId);
        equipment.forEach(e -> e.setInStock(!shortage && onHand(StockType.EQUIPMENT, sku(e.getArticle(), e.getName())) >= e.getQuantity()));
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

    /** Текущий остаток на складе для позиции (0, если позиции на складе нет). */
    @Transactional(readOnly = true)
    public int stockOnHand(StockType type, String article, String name) {
        return onHand(type, sku(article, name));
    }

    /** Все складские остатки (для просмотра). */
    @Transactional(readOnly = true)
    public List<StockItem> listStock() {
        return stockRepository.findAll();
    }

    /** Устанавливает остаток позиции на складе (создаёт позицию при отсутствии). Для демо/корректировок. */
    @Transactional
    public StockItem setStock(StockType type, String sku, String name, int quantity) {
        StockItem item = stockRepository.findByTypeAndSku(type, sku).orElseGet(() -> {
            StockItem s = new StockItem();
            s.setType(type);
            s.setSku(sku);
            s.setName(name != null && !name.isBlank() ? name : sku);
            return s;
        });
        item.setQuantityOnHand(Math.max(0, quantity));
        return stockRepository.save(item);
    }

    // --- Заказ недостающего (по-позиционные этапы) ---

    @Transactional(readOnly = true)
    public List<Part> missingParts(UUID orderId) {
        return partRepository.findByOrderId(orderId).stream().filter(p -> !p.isInStock()).toList();
    }

    @Transactional(readOnly = true)
    public List<Equipment> missingEquipment(UUID orderId) {
        return equipmentRepository.findByOrderId(orderId).stream().filter(e -> !e.isInStock()).toList();
    }

    /** Сводный статус заказа, выведенный из по-позиционных отметок. */
    @Transactional(readOnly = true)
    public SupplyOrderStatus orderStatus(UUID orderId) {
        List<Part> parts = partRepository.findByOrderId(orderId);
        List<Equipment> equipment = equipmentRepository.findByOrderId(orderId);

        boolean anyToOrder = parts.stream().anyMatch(p -> !p.isInStock() && !p.isOrdered())
                || equipment.stream().anyMatch(e -> !e.isInStock() && !e.isOrdered());
        if (anyToOrder) {
            return SupplyOrderStatus.TO_ORDER;
        }
        boolean anyWaiting = parts.stream().anyMatch(p -> p.isOrdered() && !p.isInStock())
                || equipment.stream().anyMatch(e -> e.isOrdered() && !e.isInStock());
        return anyWaiting ? SupplyOrderStatus.ORDERED : SupplyOrderStatus.RECEIVED;
    }

    /** Все недостающие позиции поступили (заявку можно вести дальше по процессу). */
    @Transactional(readOnly = true)
    public boolean allReceived(UUID orderId) {
        return missingParts(orderId).isEmpty() && missingEquipment(orderId).isEmpty();
    }

    /** Отмечает выбранные недостающие позиции заказанными. Пустой набор = все недостающие. */
    @Transactional
    public Order markOrdered(UUID orderId, Collection<UUID> partIds, Collection<UUID> equipmentIds) {
        Set<UUID> pIds = ids(partIds);
        Set<UUID> eIds = ids(equipmentIds);

        List<Part> parts = partRepository.findByOrderId(orderId);
        parts.stream()
                .filter(p -> !p.isInStock())
                .filter(p -> pIds.isEmpty() || pIds.contains(p.getId()))
                .forEach(p -> p.setOrdered(true));
        partRepository.saveAll(parts);

        List<Equipment> equipment = equipmentRepository.findByOrderId(orderId);
        equipment.stream()
                .filter(e -> !e.isInStock())
                .filter(e -> eIds.isEmpty() || eIds.contains(e.getId()))
                .forEach(e -> e.setOrdered(true));
        equipmentRepository.saveAll(equipment);

        return syncOrderStatus(orderId);
    }

    /**
     * Отмечает выбранные недостающие позиции поступившими: помечает inStock и пополняет
     * остаток склада. Пустой набор = все недостающие.
     */
    @Transactional
    public Order markReceived(UUID orderId, Collection<UUID> partIds, Collection<UUID> equipmentIds) {
        Set<UUID> pIds = ids(partIds);
        Set<UUID> eIds = ids(equipmentIds);

        List<Part> parts = partRepository.findByOrderId(orderId);
        parts.stream()
                .filter(p -> !p.isInStock())
                .filter(p -> pIds.isEmpty() || pIds.contains(p.getId()))
                .forEach(p -> {
                    replenish(StockType.PART, sku(p.getArticle(), p.getName()), p.getName(), p.getQuantity());
                    p.setOrdered(true);
                    p.setInStock(true);
                });
        partRepository.saveAll(parts);

        List<Equipment> equipment = equipmentRepository.findByOrderId(orderId);
        equipment.stream()
                .filter(e -> !e.isInStock())
                .filter(e -> eIds.isEmpty() || eIds.contains(e.getId()))
                .forEach(e -> {
                    replenish(StockType.EQUIPMENT, sku(e.getArticle(), e.getName()), e.getName(), e.getQuantity());
                    e.setOrdered(true);
                    e.setInStock(true);
                });
        equipmentRepository.saveAll(equipment);

        return syncOrderStatus(orderId);
    }

    /**
     * Совместимость: продвигает заказ целиком. {@code ORDERED} — отметить все недостающие
     * заказанными; {@code RECEIVED} — отметить все поступившими (с пополнением склада).
     */
    @Transactional
    public Order advanceOrderStatus(UUID orderId, SupplyOrderStatus target) {
        return switch (target) {
            case ORDERED -> markOrdered(orderId, Set.of(), Set.of());
            case RECEIVED -> markReceived(orderId, Set.of(), Set.of());
            case TO_ORDER -> syncOrderStatus(orderId);
        };
    }

    // --- Выдача исполнителям (списание со склада) ---

    /** Помечает выбранные позиции выданными и списывает их количество со склада. Пустой набор = всё. */
    @Transactional
    public void issue(UUID orderId, Collection<UUID> partIds, Collection<UUID> equipmentIds) {
        Set<UUID> pIds = ids(partIds);
        Set<UUID> eIds = ids(equipmentIds);

        List<Part> parts = partRepository.findByOrderId(orderId);
        parts.stream()
                .filter(p -> !p.isIssued())
                .filter(p -> pIds.isEmpty() || pIds.contains(p.getId()))
                .forEach(p -> {
                    consume(StockType.PART, sku(p.getArticle(), p.getName()), p.getQuantity());
                    p.setIssued(true);
                });
        partRepository.saveAll(parts);

        List<Equipment> equipment = equipmentRepository.findByOrderId(orderId);
        equipment.stream()
                .filter(e -> !e.isIssued())
                .filter(e -> eIds.isEmpty() || eIds.contains(e.getId()))
                .forEach(e -> {
                    consume(StockType.EQUIPMENT, sku(e.getArticle(), e.getName()), e.getQuantity());
                    e.setIssued(true);
                });
        equipmentRepository.saveAll(equipment);
    }

    // --- helpers ---

    private Order syncOrderStatus(UUID orderId) {
        Order order = requireOrder(orderId);
        order.setSupplyOrderStatus(orderStatus(orderId));
        return orderRepository.save(order);
    }

    private int onHand(StockType type, String sku) {
        return stockRepository.findByTypeAndSku(type, sku)
                .map(StockItem::getQuantityOnHand)
                .orElse(0);
    }

    private void replenish(StockType type, String sku, String name, int quantity) {
        StockItem item = stockRepository.findByTypeAndSku(type, sku).orElseGet(() -> {
            StockItem s = new StockItem();
            s.setType(type);
            s.setSku(sku);
            s.setName(name);
            s.setQuantityOnHand(0);
            return s;
        });
        item.setQuantityOnHand(item.getQuantityOnHand() + Math.max(quantity, 1));
        stockRepository.save(item);
    }

    private void consume(StockType type, String sku, int quantity) {
        stockRepository.findByTypeAndSku(type, sku).ifPresent(item -> {
            item.setQuantityOnHand(Math.max(0, item.getQuantityOnHand() - Math.max(quantity, 1)));
            stockRepository.save(item);
        });
    }

    private static String sku(String article, String name) {
        return article != null && !article.isBlank() ? article.trim() : name;
    }

    private static Set<UUID> ids(Collection<UUID> ids) {
        return ids != null ? Set.copyOf(ids) : Set.of();
    }

    private Order requireOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}