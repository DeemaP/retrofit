package com.adas.retrofit.service;

import com.adas.retrofit.entity.CalibrationRecord;
import com.adas.retrofit.entity.CalibrationType;
import com.adas.retrofit.entity.Order;
import com.adas.retrofit.entity.ProgrammingDirection;
import com.adas.retrofit.entity.ProgrammingRecord;
import com.adas.retrofit.entity.WorkChecklistItem;
import com.adas.retrofit.entity.WorkPhase;
import com.adas.retrofit.repository.CalibrationRecordRepository;
import com.adas.retrofit.repository.OrderRepository;
import com.adas.retrofit.repository.ProgrammingRecordRepository;
import com.adas.retrofit.repository.WorkChecklistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Технические работы по заявке: программирование ЭБУ, калибровка и чек-листы
 * слесарно-монтажных работ (монтаж/демонтаж). Обслуживает ЮТ «Монтажные и слесарные работы»,
 * «Кодирование и калибровка», «Демонтаж установленной системы» и
 * «Возврат авто в изначальное состояние».
 */
@Service
public class WorkService {

    /** Этапы чек-листа по умолчанию для монтажных работ. */
    private static final List<String> ASSEMBLY_STEPS = List.of(
            "Разборка (демонтаж штатных элементов)",
            "Монтаж оборудования",
            "Сборка",
            "Проверка корректности установки и сборки");

    /** Этапы чек-листа по умолчанию для демонтажа. */
    private static final List<String> DISASSEMBLY_STEPS = List.of(
            "Разборка",
            "Демонтаж установленного оборудования",
            "Сборка (возврат штатных элементов)",
            "Проверка корректности сборки");

    private final OrderRepository orderRepository;
    private final ProgrammingRecordRepository programmingRepository;
    private final CalibrationRecordRepository calibrationRepository;
    private final WorkChecklistItemRepository checklistRepository;

    public WorkService(OrderRepository orderRepository,
                       ProgrammingRecordRepository programmingRepository,
                       CalibrationRecordRepository calibrationRepository,
                       WorkChecklistItemRepository checklistRepository) {
        this.orderRepository = orderRepository;
        this.programmingRepository = programmingRepository;
        this.calibrationRepository = calibrationRepository;
        this.checklistRepository = checklistRepository;
    }

    // --- Программирование ЭБУ ---

    @Transactional
    public ProgrammingRecord saveProgramming(UUID orderId, String ecuName, String swBefore,
                                             String swAfter, String activatedFeatures,
                                             ProgrammingDirection direction) {
        Order order = requireOrder(orderId);
        ProgrammingRecord record = new ProgrammingRecord();
        record.setOrder(order);
        record.setEcuName(ecuName);
        record.setSwBefore(swBefore);
        record.setSwAfter(swAfter);
        record.setActivatedFeatures(activatedFeatures);
        record.setDirection(direction != null ? direction : ProgrammingDirection.INSTALL);
        return programmingRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<ProgrammingRecord> programmingOf(UUID orderId) {
        return programmingRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    // --- Калибровка ---

    @Transactional
    public CalibrationRecord saveCalibration(UUID orderId, CalibrationType type,
                                             String parameters, boolean passed) {
        Order order = requireOrder(orderId);
        CalibrationRecord record = new CalibrationRecord();
        record.setOrder(order);
        record.setType(type != null ? type : CalibrationType.STATIC);
        record.setParameters(parameters);
        record.setPassed(passed);
        return calibrationRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<CalibrationRecord> calibrationOf(UUID orderId) {
        return calibrationRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    // --- Чек-лист слесарно-монтажных работ ---

    /** Текущий чек-лист фазы; если пуст — отдаёт предустановленные этапы (не невыполненными). */
    @Transactional(readOnly = true)
    public List<WorkChecklistItem> checklist(UUID orderId, WorkPhase phase) {
        List<WorkChecklistItem> saved = checklistRepository.findByOrderIdAndPhaseOrderByPositionAsc(orderId, phase);
        if (!saved.isEmpty()) {
            return saved;
        }
        return defaultSteps(phase).stream().map(title -> {
            WorkChecklistItem item = new WorkChecklistItem();
            item.setPhase(phase);
            item.setTitle(title);
            item.setDone(false);
            return item;
        }).toList();
    }

    /** Полностью заменяет чек-лист фазы переданными пунктами. */
    @Transactional
    public void saveChecklist(UUID orderId, WorkPhase phase, List<ChecklistEntry> entries) {
        Order order = requireOrder(orderId);
        checklistRepository.deleteByOrderIdAndPhase(orderId, phase);
        List<ChecklistEntry> source = (entries != null && !entries.isEmpty())
                ? entries
                : defaultSteps(phase).stream().map(t -> new ChecklistEntry(t, false)).toList();
        int pos = 0;
        for (ChecklistEntry entry : source) {
            if (entry == null || entry.title() == null || entry.title().isBlank()) {
                continue;
            }
            WorkChecklistItem item = new WorkChecklistItem();
            item.setOrder(order);
            item.setPhase(phase);
            item.setTitle(entry.title().trim());
            item.setDone(entry.done());
            item.setPosition(pos++);
            checklistRepository.save(item);
        }
    }

    private List<String> defaultSteps(WorkPhase phase) {
        return phase == WorkPhase.DISASSEMBLY ? DISASSEMBLY_STEPS : ASSEMBLY_STEPS;
    }

    private Order requireOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /** Лёгкий носитель пункта чек-листа между слоями (заголовок + признак выполнения). */
    public record ChecklistEntry(String title, boolean done) {
    }
}