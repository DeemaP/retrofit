package com.adas.retrofit.delegate;

import com.adas.retrofit.service.SupplyService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Проверка наличия запчастей и оборудования на складе (Activity_1wfd5h1).
 * Выставляет раздельные переменные {@code partsAvailable} и {@code equipmentAvailable}.
 */
@Component("checkStockDelegate")
public class CheckStockDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CheckStockDelegate.class);

    private final SupplyService supplyService;

    public CheckStockDelegate(SupplyService supplyService) {
        this.supplyService = supplyService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        UUID orderId = UUID.fromString((String) execution.getVariable(ProcessVariables.ORDER_ID));
        boolean partsShortage = Boolean.TRUE.equals(execution.getVariable(ProcessVariables.PARTS_SHORTAGE));
        boolean equipmentShortage = Boolean.TRUE.equals(execution.getVariable(ProcessVariables.EQUIPMENT_SHORTAGE));
        boolean partsAvailable = supplyService.checkPartsStock(orderId, partsShortage);
        boolean equipmentAvailable = supplyService.checkEquipmentStock(orderId, equipmentShortage);
        execution.setVariable(ProcessVariables.PARTS_AVAILABLE, partsAvailable);
        execution.setVariable(ProcessVariables.EQUIPMENT_AVAILABLE, equipmentAvailable);
        log.info("Проверка склада для orderId={}: запчасти={}, оборудование={}",
                orderId, partsAvailable, equipmentAvailable);
    }
}
