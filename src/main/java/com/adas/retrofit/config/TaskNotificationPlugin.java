package com.adas.retrofit.config;

import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Регистрирует {@link TaskNotificationParseListener} как pre-BPMN-parse listener
 * движка. Бины {@code ProcessEnginePlugin} автоматически подхватываются
 * camunda-bpm-spring-boot-starter.
 */
@Component
public class TaskNotificationPlugin extends AbstractProcessEnginePlugin {

    private final TaskListener taskNotificationListener;

    public TaskNotificationPlugin(TaskListener taskNotificationListener) {
        this.taskNotificationListener = taskNotificationListener;
    }

    @Override
    public void preInit(ProcessEngineConfigurationImpl configuration) {
        if (configuration.getCustomPreBPMNParseListeners() == null) {
            configuration.setCustomPreBPMNParseListeners(new ArrayList<>());
        }
        configuration.getCustomPreBPMNParseListeners()
                .add(new TaskNotificationParseListener(taskNotificationListener));
    }
}