package com.adas.retrofit.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Сохранение списков запчастей/оборудования и информации о проекте в БД.
 * Для альфы — логирует факт сохранения.
 */
@Component("saveToDbDelegate")
public class SaveToDbDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SaveToDbDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Сохранение в БД [{}] для orderId={}",
                execution.getCurrentActivityName(), execution.getVariable(ProcessVariables.ORDER_ID));
    }
}