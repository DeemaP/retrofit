package com.adas.retrofit.delegate;

import com.adas.retrofit.service.SpecCatalogService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Проверка БД на наличие справочника <b>запчастей</b> под конфигурацию авто (Activity_1auf6ys):
 * модель + год + тип дооснащения. Выставляет переменную {@code partsListInDb}. Если справочник
 * есть — сразу разворачивает его в список под заявку (на «true»-ветке формирование пропускается).
 */
@Component("checkPartsListDelegate")
public class CheckPartsListDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CheckPartsListDelegate.class);

    private final SpecCatalogService specCatalogService;

    public CheckPartsListDelegate(SpecCatalogService specCatalogService) {
        this.specCatalogService = specCatalogService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        UUID orderId = UUID.fromString((String) execution.getVariable(ProcessVariables.ORDER_ID));
        boolean exists = specCatalogService.partsSpecExists(orderId);
        execution.setVariable(ProcessVariables.PARTS_LIST_IN_DB, exists);
        if (exists) {
            specCatalogService.materializeParts(orderId);
        }
        log.info("Проверка БД: справочник запчастей для orderId={} {}", orderId, exists ? "найден" : "отсутствует");
    }
}
