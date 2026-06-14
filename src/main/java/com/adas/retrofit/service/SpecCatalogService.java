package com.adas.retrofit.service;

import com.adas.retrofit.dto.SupplyItem;
import com.adas.retrofit.entity.Equipment;
import com.adas.retrofit.entity.EquipmentSpec;
import com.adas.retrofit.entity.Order;
import com.adas.retrofit.entity.Part;
import com.adas.retrofit.entity.PartSpec;
import com.adas.retrofit.entity.RetrofitType;
import com.adas.retrofit.entity.Vehicle;
import com.adas.retrofit.repository.EquipmentRepository;
import com.adas.retrofit.repository.EquipmentSpecRepository;
import com.adas.retrofit.repository.OrderRepository;
import com.adas.retrofit.repository.PartRepository;
import com.adas.retrofit.repository.PartSpecRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Справочник требуемых списков, привязанный к паре «модель + год выпуска + тип дооснащения».
 *
 * <p>Логика этапов «Проверка БД» / «Формирование списка» / «Сохранение списка»:
 * <ul>
 *   <li>{@link #partsSpecExists}/{@link #equipmentSpecExists} — есть ли справочник под конфигурацию авто;</li>
 *   <li>форма ЮТ сохраняет справочник ({@link #savePartsSpec}/{@link #saveEquipmentSpec});</li>
 *   <li>{@link #materializeParts}/{@link #materializeEquipment} разворачивают справочник в список
 *       под конкретную заявку. Если справочника нет, он засеивается из {@link RetrofitCatalog}
 *       (чтобы happy-path работал и при пустой форме / завершении из Cockpit).</li>
 * </ul>
 */
@Service
public class SpecCatalogService {

    private final OrderRepository orderRepository;
    private final PartRepository partRepository;
    private final EquipmentRepository equipmentRepository;
    private final PartSpecRepository partSpecRepository;
    private final EquipmentSpecRepository equipmentSpecRepository;

    public SpecCatalogService(OrderRepository orderRepository,
                              PartRepository partRepository,
                              EquipmentRepository equipmentRepository,
                              PartSpecRepository partSpecRepository,
                              EquipmentSpecRepository equipmentSpecRepository) {
        this.orderRepository = orderRepository;
        this.partRepository = partRepository;
        this.equipmentRepository = equipmentRepository;
        this.partSpecRepository = partSpecRepository;
        this.equipmentSpecRepository = equipmentSpecRepository;
    }

    // --- Запчасти ---

    @Transactional(readOnly = true)
    public boolean partsSpecExists(UUID orderId) {
        Vehicle v = requireOrder(orderId).getVehicle();
        return partSpecRepository.existsByModelAndProductionYearAndRetrofitType(
                model(v), year(v), type(orderId));
    }

    /** Подсказка для формы: существующий справочник или дефолт из {@link RetrofitCatalog}. */
    @Transactional(readOnly = true)
    public List<SupplyItem> suggestParts(UUID orderId) {
        Order order = requireOrder(orderId);
        Vehicle v = order.getVehicle();
        List<PartSpec> spec = partSpecRepository.findByModelAndProductionYearAndRetrofitType(
                model(v), year(v), order.getRetrofitType());
        if (!spec.isEmpty()) {
            return spec.stream().map(s -> new SupplyItem(s.getName(), s.getArticle(), s.getQuantity())).toList();
        }
        return RetrofitCatalog.partsFor(order.getRetrofitType()).stream()
                .map(name -> new SupplyItem(name, null, 1)).toList();
    }

    /** Сохраняет справочник запчастей под конфигурацию авто заявки (заменяет прежний). */
    @Transactional
    public void savePartsSpec(UUID orderId, List<SupplyItem> items) {
        Order order = requireOrder(orderId);
        Vehicle v = order.getVehicle();
        partSpecRepository.deleteByModelAndProductionYearAndRetrofitType(model(v), year(v), order.getRetrofitType());
        partSpecRepository.flush();
        List<PartSpec> specs = items.stream()
                .filter(i -> i.name() != null && !i.name().isBlank())
                .map(i -> {
                    PartSpec s = new PartSpec();
                    s.setModel(model(v));
                    s.setProductionYear(year(v));
                    s.setRetrofitType(order.getRetrofitType());
                    s.setName(i.name().trim());
                    s.setArticle(i.article() != null ? i.article().trim() : null);
                    s.setQuantity(i.quantity() != null && i.quantity() > 0 ? i.quantity() : 1);
                    return s;
                })
                .toList();
        partSpecRepository.saveAll(specs);
    }

    /** Разворачивает справочник запчастей в список под заявку (с засевом из каталога при отсутствии). */
    @Transactional
    public List<Part> materializeParts(UUID orderId) {
        Order order = requireOrder(orderId);
        Vehicle v = order.getVehicle();
        List<PartSpec> spec = partSpecRepository.findByModelAndProductionYearAndRetrofitType(
                model(v), year(v), order.getRetrofitType());
        if (spec.isEmpty()) {
            spec = seedPartsSpec(order);
        }
        partRepository.deleteByOrderId(orderId);
        partRepository.flush();
        List<Part> parts = spec.stream().map(s -> {
            Part p = new Part();
            p.setOrder(order);
            p.setName(s.getName());
            p.setArticle(s.getArticle());
            p.setQuantity(s.getQuantity());
            return p;
        }).toList();
        return partRepository.saveAll(parts);
    }

    private List<PartSpec> seedPartsSpec(Order order) {
        Vehicle v = order.getVehicle();
        List<PartSpec> specs = RetrofitCatalog.partsFor(order.getRetrofitType()).stream()
                .map(name -> {
                    PartSpec s = new PartSpec();
                    s.setModel(model(v));
                    s.setProductionYear(year(v));
                    s.setRetrofitType(order.getRetrofitType());
                    s.setName(name);
                    s.setQuantity(1);
                    return s;
                })
                .toList();
        return partSpecRepository.saveAll(specs);
    }

    // --- Оборудование ---

    @Transactional(readOnly = true)
    public boolean equipmentSpecExists(UUID orderId) {
        Vehicle v = requireOrder(orderId).getVehicle();
        return equipmentSpecRepository.existsByModelAndProductionYearAndRetrofitType(
                model(v), year(v), type(orderId));
    }

    @Transactional(readOnly = true)
    public List<SupplyItem> suggestEquipment(UUID orderId) {
        Order order = requireOrder(orderId);
        Vehicle v = order.getVehicle();
        List<EquipmentSpec> spec = equipmentSpecRepository.findByModelAndProductionYearAndRetrofitType(
                model(v), year(v), order.getRetrofitType());
        if (!spec.isEmpty()) {
            return spec.stream().map(s -> new SupplyItem(s.getName(), s.getArticle(), 1)).toList();
        }
        return RetrofitCatalog.equipmentFor(order.getRetrofitType()).stream()
                .map(name -> new SupplyItem(name, null, 1)).toList();
    }

    @Transactional
    public void saveEquipmentSpec(UUID orderId, List<SupplyItem> items) {
        Order order = requireOrder(orderId);
        Vehicle v = order.getVehicle();
        equipmentSpecRepository.deleteByModelAndProductionYearAndRetrofitType(model(v), year(v), order.getRetrofitType());
        equipmentSpecRepository.flush();
        List<EquipmentSpec> specs = items.stream()
                .filter(i -> i.name() != null && !i.name().isBlank())
                .map(i -> {
                    EquipmentSpec s = new EquipmentSpec();
                    s.setModel(model(v));
                    s.setProductionYear(year(v));
                    s.setRetrofitType(order.getRetrofitType());
                    s.setName(i.name().trim());
                    s.setArticle(i.article() != null ? i.article().trim() : null);
                    return s;
                })
                .toList();
        equipmentSpecRepository.saveAll(specs);
    }

    @Transactional
    public List<Equipment> materializeEquipment(UUID orderId) {
        Order order = requireOrder(orderId);
        Vehicle v = order.getVehicle();
        List<EquipmentSpec> spec = equipmentSpecRepository.findByModelAndProductionYearAndRetrofitType(
                model(v), year(v), order.getRetrofitType());
        if (spec.isEmpty()) {
            spec = seedEquipmentSpec(order);
        }
        equipmentRepository.deleteByOrderId(orderId);
        equipmentRepository.flush();
        List<Equipment> equipment = spec.stream().map(s -> {
            Equipment e = new Equipment();
            e.setOrder(order);
            e.setName(s.getName());
            e.setArticle(s.getArticle());
            return e;
        }).toList();
        return equipmentRepository.saveAll(equipment);
    }

    private List<EquipmentSpec> seedEquipmentSpec(Order order) {
        Vehicle v = order.getVehicle();
        List<EquipmentSpec> specs = RetrofitCatalog.equipmentFor(order.getRetrofitType()).stream()
                .map(name -> {
                    EquipmentSpec s = new EquipmentSpec();
                    s.setModel(model(v));
                    s.setProductionYear(year(v));
                    s.setRetrofitType(order.getRetrofitType());
                    s.setName(name);
                    return s;
                })
                .toList();
        return equipmentSpecRepository.saveAll(specs);
    }

    // --- helpers ---

    private Order requireOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private static String model(Vehicle v) {
        return v != null && v.getModel() != null ? v.getModel() : "UNKNOWN";
    }

    private static Integer year(Vehicle v) {
        return v != null ? v.getYear() : null;
    }

    private RetrofitType type(UUID orderId) {
        return requireOrder(orderId).getRetrofitType();
    }
}