package com.adas.retrofit.delegate;

import com.adas.retrofit.service.SpecCatalogService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Сохранение списка <b>оборудования</b> заявки в БД (Activity_1vm038x): разворачивает справочник
 * (заполненный формой или засеянный из каталога) в список под конкретную заявку.
 */
@Component("saveEquipmentListDelegate")
public class SaveEquipmentListDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SaveEquipmentListDelegate.class);

    private final SpecCatalogService specCatalogService;

    public SaveEquipmentListDelegate(SpecCatalogService specCatalogService) {
        this.specCatalogService = specCatalogService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        UUID orderId = UUID.fromString((String) execution.getVariable(ProcessVariables.ORDER_ID));
        int count = specCatalogService.materializeEquipment(orderId).size();
        log.info("Развёрнут список оборудования ({} поз.) для orderId={}", count, orderId);
    }
}
