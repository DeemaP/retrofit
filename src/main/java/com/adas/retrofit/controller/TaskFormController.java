package com.adas.retrofit.controller;

import com.adas.retrofit.delegate.ProcessVariables;
import com.adas.retrofit.dto.AcceptanceRequest;
import com.adas.retrofit.dto.AdvanceOrderRequest;
import com.adas.retrofit.dto.CalibrationView;
import com.adas.retrofit.dto.ChecklistItemDto;
import com.adas.retrofit.dto.ChecklistItemView;
import com.adas.retrofit.dto.ChecklistRequest;
import com.adas.retrofit.dto.CodingRequest;
import com.adas.retrofit.dto.FeasibilityRequest;
import com.adas.retrofit.dto.IssueRequest;
import com.adas.retrofit.dto.OrderSupplyView;
import com.adas.retrofit.dto.PhotoView;
import com.adas.retrofit.dto.ProgrammingRequest;
import com.adas.retrofit.dto.ProgrammingView;
import com.adas.retrofit.dto.RequirementsRequest;
import com.adas.retrofit.dto.SupplyItem;
import com.adas.retrofit.dto.SupplyListRequest;
import com.adas.retrofit.dto.SupplyMarkRequest;
import com.adas.retrofit.entity.DamagePhoto;
import com.adas.retrofit.entity.Equipment;
import com.adas.retrofit.entity.Part;
import com.adas.retrofit.entity.ProgrammingDirection;
import com.adas.retrofit.entity.StockType;
import com.adas.retrofit.entity.SupplyOrderStatus;
import com.adas.retrofit.entity.WorkPhase;
import com.adas.retrofit.service.OrderService;
import com.adas.retrofit.service.PhotoStorageService;
import com.adas.retrofit.service.SpecCatalogService;
import com.adas.retrofit.service.SupplyService;
import com.adas.retrofit.service.WorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.camunda.bpm.engine.TaskService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Эндпоинты форм user task: сохраняют доменные данные шага и (если передан taskId)
 * завершают соответствующую задачу процесса, двигая его дальше.
 */
@RestController
@RequestMapping("/api/v1/orders/{orderId}")
@Tag(name = "Task forms", description = "Формы user task с привязкой к заявке")
public class TaskFormController {

    private final OrderService orderService;
    private final SupplyService supplyService;
    private final SpecCatalogService specCatalogService;
    private final PhotoStorageService photoStorageService;
    private final WorkService workService;
    private final TaskService taskService;

    public TaskFormController(OrderService orderService,
                              SupplyService supplyService,
                              SpecCatalogService specCatalogService,
                              PhotoStorageService photoStorageService,
                              WorkService workService,
                              TaskService taskService) {
        this.orderService = orderService;
        this.supplyService = supplyService;
        this.specCatalogService = specCatalogService;
        this.photoStorageService = photoStorageService;
        this.workService = workService;
        this.taskService = taskService;
    }

    // --- 1. Первичная приёмка ---

    @PostMapping("/acceptance")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Первичная приёмка: пробег, госномер, СТС + завершение задачи")
    public void acceptance(@PathVariable UUID orderId, @RequestBody AcceptanceRequest req) {
        orderService.saveAcceptance(orderId, req.mileage(), req.licensePlate(), req.stsNumber());
        completeIfPresent(req.taskId(), Map.of());
    }

    // --- 2. Фиксация ТЗ ---

    @PostMapping("/requirements")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Фиксация ТЗ/пожеланий клиента + завершение задачи")
    public void requirements(@PathVariable UUID orderId, @RequestBody RequirementsRequest req) {
        orderService.saveRequirements(orderId, req.clientRequirements());
        completeIfPresent(req.taskId(), Map.of());
    }

    // --- 3. Фотофиксация ---

    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить фотографии повреждений (одну или несколько)")
    public List<PhotoView> uploadPhotos(@PathVariable UUID orderId,
                                        @RequestParam("files") MultipartFile[] files) {
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                photoStorageService.upload(orderId, file);
            }
        }
        return photoStorageService.listPhotos(orderId).stream().map(PhotoView::of).toList();
    }

    @GetMapping("/photos")
    @Operation(summary = "Список загруженных фото повреждений заявки")
    public List<PhotoView> listPhotos(@PathVariable UUID orderId) {
        return photoStorageService.listPhotos(orderId).stream().map(PhotoView::of).toList();
    }

    @GetMapping("/photos/{photoId}/content")
    @Operation(summary = "Получить байты фото (проксируется из MinIO)")
    public ResponseEntity<InputStreamResource> photoContent(@PathVariable UUID orderId,
                                                            @PathVariable UUID photoId) {
        DamagePhoto photo = photoStorageService.requirePhoto(photoId);
        MediaType mediaType = photo.getContentType() != null
                ? MediaType.parseMediaType(photo.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + photo.getOriginalFilename() + "\"")
                .body(new InputStreamResource(photoStorageService.openContent(photo)));
    }

    // --- 4. Проверка возможности дооснащения ---

    @PostMapping("/feasibility")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Возможность дооснащения (можно/нельзя + причина) + завершение задачи")
    public void feasibility(@PathVariable UUID orderId, @RequestBody FeasibilityRequest req) {
        orderService.saveFeasibility(orderId, req.possible(), req.comment());
        completeIfPresent(req.taskId(),
                Map.of(ProcessVariables.RETROFIT_POSSIBLE, req.possible()));
    }

    // --- 5. Список запчастей ---

    @GetMapping("/parts/suggestion")
    @Operation(summary = "Подсказка для формы запчастей (справочник модели/года/типа или каталог)")
    public List<SupplyItem> partsSuggestion(@PathVariable UUID orderId) {
        return specCatalogService.suggestParts(orderId);
    }

    @PostMapping("/parts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Сохранить справочник запчастей (модель/год/тип), развернуть под заявку + завершить")
    public void parts(@PathVariable UUID orderId, @RequestBody SupplyListRequest req) {
        specCatalogService.savePartsSpec(orderId, req.items() != null ? req.items() : List.of());
        specCatalogService.materializeParts(orderId);
        completeIfPresent(req.taskId(), Map.of());
    }

    // --- 6. Список оборудования ---

    @GetMapping("/equipment/suggestion")
    @Operation(summary = "Подсказка для формы оборудования (справочник модели/года/типа или каталог)")
    public List<SupplyItem> equipmentSuggestion(@PathVariable UUID orderId) {
        return specCatalogService.suggestEquipment(orderId);
    }

    @PostMapping("/equipment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Сохранить справочник оборудования (модель/год/тип), развернуть под заявку + завершить")
    public void equipment(@PathVariable UUID orderId, @RequestBody SupplyListRequest req) {
        specCatalogService.saveEquipmentSpec(orderId, req.items() != null ? req.items() : List.of());
        specCatalogService.materializeEquipment(orderId);
        completeIfPresent(req.taskId(), Map.of());
    }

    // --- 7. Заказ недостающих запчастей и оборудования (статусы) ---

    @GetMapping("/order-supply")
    @Operation(summary = "Что нужно заказать (отсутствующее) + сводный статус заказа")
    public OrderSupplyView orderSupply(@PathVariable UUID orderId) {
        return OrderSupplyView.of(
                supplyService.orderStatus(orderId),
                supplyService.missingParts(orderId),
                supplyService.missingEquipment(orderId),
                this::partStock, this::equipmentStock);
    }

    @PostMapping("/order-supply/mark-ordered")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Отметить выбранные позиции заказанными (пустые списки = все недостающие)")
    public void markOrdered(@PathVariable UUID orderId, @RequestBody SupplyMarkRequest req) {
        supplyService.markOrdered(orderId, req.partIds(), req.equipmentIds());
    }

    @PostMapping("/order-supply/mark-received")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Отметить выбранные позиции поступившими (пополнить склад); "
            + "когда поступило всё и передан taskId — завершить задачу")
    public void markReceived(@PathVariable UUID orderId, @RequestBody SupplyMarkRequest req) {
        supplyService.markReceived(orderId, req.partIds(), req.equipmentIds());
        if (supplyService.allReceived(orderId)) {
            completeIfPresent(req.taskId(), Map.of());
        }
    }

    @PostMapping("/order-supply/advance")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Продвинуть заказ целиком; на RECEIVED (с taskId) завершить задачу")
    public void advanceOrder(@PathVariable UUID orderId, @RequestBody AdvanceOrderRequest req) {
        supplyService.advanceOrderStatus(orderId, req.status());
        if (req.status() == SupplyOrderStatus.RECEIVED && supplyService.allReceived(orderId)) {
            completeIfPresent(req.taskId(), Map.of());
        }
    }

    // --- 8. Выдача запчастей и оборудования исполнителям ---

    @PostMapping("/issue")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Выдать (списать со склада) выбранные позиции + завершить задачу")
    public void issue(@PathVariable UUID orderId, @RequestBody IssueRequest req) {
        supplyService.issue(orderId, req.partIds(), req.equipmentIds());
        completeIfPresent(req.taskId(), Map.of());
    }

    // --- 9. Монтажные и слесарные работы (чек-лист) ---

    @GetMapping("/assembly")
    @Operation(summary = "Чек-лист монтажных работ (сохранённый или предустановленные этапы)")
    public List<ChecklistItemView> assembly(@PathVariable UUID orderId) {
        return workService.checklist(orderId, WorkPhase.ASSEMBLY).stream()
                .map(ChecklistItemView::of).toList();
    }

    @PostMapping("/assembly")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Сохранить чек-лист монтажа (промежуточно без taskId; с taskId — завершить при полном заполнении)")
    public void saveAssembly(@PathVariable UUID orderId, @RequestBody ChecklistRequest req) {
        workService.saveChecklist(orderId, WorkPhase.ASSEMBLY, toEntries(req.items()));
        completeChecklistIfRequested(req);
    }

    // --- 10. Кодирование и калибровка ---

    @GetMapping("/programming")
    @Operation(summary = "Записи программирования ЭБУ по заявке")
    public List<ProgrammingView> programming(@PathVariable UUID orderId) {
        return workService.programmingOf(orderId).stream().map(ProgrammingView::of).toList();
    }

    @GetMapping("/calibration")
    @Operation(summary = "Записи калибровки по заявке")
    public List<CalibrationView> calibration(@PathVariable UUID orderId) {
        return workService.calibrationOf(orderId).stream().map(CalibrationView::of).toList();
    }

    @PostMapping("/coding")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Кодирование ЭБУ + калибровка; systemHealthy управляет гейтвеем + завершение")
    public void coding(@PathVariable UUID orderId, @RequestBody CodingRequest req) {
        workService.saveProgramming(orderId, req.ecuName(), req.swBefore(), req.swAfter(),
                req.activatedFeatures(), ProgrammingDirection.INSTALL);
        workService.saveCalibration(orderId, req.calibrationType(),
                req.calibrationParameters(), req.calibrationPassed());
        completeIfPresent(req.taskId(),
                Map.of(ProcessVariables.SYSTEM_HEALTHY, req.systemHealthy()));
    }

    // --- 11. Демонтаж установленной системы (чек-лист) ---

    @GetMapping("/disassembly")
    @Operation(summary = "Чек-лист демонтажа (сохранённый или предустановленные этапы)")
    public List<ChecklistItemView> disassembly(@PathVariable UUID orderId) {
        return workService.checklist(orderId, WorkPhase.DISASSEMBLY).stream()
                .map(ChecklistItemView::of).toList();
    }

    @PostMapping("/disassembly")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Сохранить чек-лист демонтажа (промежуточно без taskId; с taskId — завершить при полном заполнении)")
    public void saveDisassembly(@PathVariable UUID orderId, @RequestBody ChecklistRequest req) {
        workService.saveChecklist(orderId, WorkPhase.DISASSEMBLY, toEntries(req.items()));
        completeChecklistIfRequested(req);
    }

    /**
     * Завершает задачу чек-листа только если передан taskId и ВСЕ пункты отмечены выполненными.
     * Без taskId — промежуточное сохранение (задача остаётся активной). Неполный чек-лист
     * при попытке завершения → 400.
     */
    private void completeChecklistIfRequested(ChecklistRequest req) {
        if (req.taskId() == null || req.taskId().isBlank()) {
            return;
        }
        List<ChecklistItemDto> items = req.items();
        boolean allDone = items != null && !items.isEmpty()
                && items.stream().allMatch(ChecklistItemDto::done);
        if (!allDone) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Нельзя завершить задачу: чек-лист заполнен не полностью");
        }
        taskService.complete(req.taskId(), Map.of());
    }

    // --- 12. Возврат авто в изначальное состояние (программирование RESTORE) ---

    @PostMapping("/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Восстановление исходного ПО ЭБУ (RESTORE) + завершение задачи")
    public void restore(@PathVariable UUID orderId, @RequestBody ProgrammingRequest req) {
        workService.saveProgramming(orderId, req.ecuName(), req.swBefore(), req.swAfter(),
                req.activatedFeatures(), ProgrammingDirection.RESTORE);
        completeIfPresent(req.taskId(), Map.of());
    }

    private static List<WorkService.ChecklistEntry> toEntries(List<ChecklistItemDto> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(i -> new WorkService.ChecklistEntry(i.title(), i.done()))
                .toList();
    }

    private void completeIfPresent(String taskId, Map<String, Object> variables) {
        if (taskId != null && !taskId.isBlank()) {
            taskService.complete(taskId, variables);
        }
    }

    private int partStock(Part p) {
        return supplyService.stockOnHand(StockType.PART, p.getArticle(), p.getName());
    }

    private int equipmentStock(Equipment e) {
        return supplyService.stockOnHand(StockType.EQUIPMENT, e.getArticle(), e.getName());
    }
}