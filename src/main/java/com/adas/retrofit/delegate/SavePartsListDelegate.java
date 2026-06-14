package com.adas.retrofit.delegate;

import com.adas.retrofit.service.SpecCatalogService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Сохранение списка <b>запчастей</b> заявки в БД (Activity_0f4d1wp): разворачивает справочник
 * (заполненный формой или засеянный из каталога) в список под конкретную заявку.
 */
@Component("savePartsListDelegate")
public class SavePartsListDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SavePartsListDelegate.class);

    private final SpecCatalogService specCatalogService;

    public SavePartsListDelegate(SpecCatalogService specCatalogService) {
        this.specCatalogService = specCatalogService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        UUID orderId = UUID.fromString((String) execution.getVariable(ProcessVariables.ORDER_ID));
        int count = specCatalogService.materializeParts(orderId).size();
        log.info("Развёрнут список запчастей ({} поз.) для orderId={}", count, orderId);
    }
}
