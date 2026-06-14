package com.adas.retrofit.delegate;

import com.adas.retrofit.entity.ComplianceAct;
import com.adas.retrofit.service.ActService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Подготовка документации и актов для передачи клиенту.
 * Создаёт ComplianceAct, привязанный к Order (orderId берётся из переменных процесса).
 */
@Component("generateDocumentationDelegate")
public class GenerateDocumentationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(GenerateDocumentationDelegate.class);

    private final ActService actService;

    public GenerateDocumentationDelegate(ActService actService) {
        this.actService = actService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        UUID orderId = UUID.fromString((String) execution.getVariable(ProcessVariables.ORDER_ID));
        ComplianceAct act = actService.createForOrder(orderId);
        log.info("Подготовлена документация: акт {} для orderId={}", act.getDocumentNumber(), orderId);
    }
}