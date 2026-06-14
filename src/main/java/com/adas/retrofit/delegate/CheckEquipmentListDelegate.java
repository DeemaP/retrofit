package com.adas.retrofit.delegate;

import com.adas.retrofit.service.SpecCatalogService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Проверка БД на наличие справочника <b>оборудования</b> под конфигурацию авто (Activity_1j7m77i):
 * модель + год + тип дооснащения. Выставляет переменную {@code equipmentListInDb}. Если справочник
 * есть — сразу разворачивает его в список под заявку.
 */
@Component("checkEquipmentListDelegate")
public class CheckEquipmentListDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CheckEquipmentListDelegate.class);

    private final SpecCatalogService specCatalogService;

    public CheckEquipmentListDelegate(SpecCatalogService specCatalogService) {
        this.specCatalogService = specCatalogService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        UUID orderId = UUID.fromString((String) execution.getVariable(ProcessVariables.ORDER_ID));
        boolean exists = specCatalogService.equipmentSpecExists(orderId);
        execution.setVariable(ProcessVariables.EQUIPMENT_LIST_IN_DB, exists);
        if (exists) {
            specCatalogService.materializeEquipment(orderId);
        }
        log.info("Проверка БД: справочник оборудования для orderId={} {}", orderId, exists ? "найден" : "отсутствует");
    }
}
