package com.adas.retrofit.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Универсальная заглушка для нереализованных в альфе ветвей процесса.
 * Просто логирует факт выполнения и завершается, чтобы процесс не падал.
 */
@Component("stubDelegate")
public class StubDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(StubDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        log.info("[STUB] executed: {}", execution.getCurrentActivityName());
    }
}