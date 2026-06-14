package com.adas.retrofit.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Формирование акта о приёме и предварительного заказ-наряда.
 * Для альфы — генерирует номер акта и кладёт его в переменную процесса.
 */
@Component("formAcceptanceActDelegate")
public class FormAcceptanceActDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(FormAcceptanceActDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String acceptanceActNumber = "ACC-" + execution.getProcessInstanceId().substring(0, 8);
        execution.setVariable(ProcessVariables.ACCEPTANCE_ACT_NUMBER, acceptanceActNumber);
        log.info("Сформирован акт о приёме {} (orderId={})",
                acceptanceActNumber, execution.getVariable(ProcessVariables.ORDER_ID));
    }
}
