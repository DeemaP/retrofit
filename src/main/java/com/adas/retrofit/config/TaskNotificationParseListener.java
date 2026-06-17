package com.adas.retrofit.config;

import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.task.TaskDefinition;
import org.camunda.bpm.engine.impl.util.xml.Element;

/**
 * При парсинге BPMN навешивает {@link TaskNotificationListener} на событие
 * {@code create} КАЖДОЙ user task. Так оповещения работают для всех задач модели,
 * не требуя правки самой BPMN (модель утверждена научным руководителем).
 */
public class TaskNotificationParseListener extends AbstractBpmnParseListener {

    private final TaskListener taskNotificationListener;

    public TaskNotificationParseListener(TaskListener taskNotificationListener) {
        this.taskNotificationListener = taskNotificationListener;
    }

    @Override
    public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
        UserTaskActivityBehavior behavior = (UserTaskActivityBehavior) activity.getActivityBehavior();
        TaskDefinition taskDefinition = behavior.getTaskDefinition();
        taskDefinition.addTaskListener(TaskListener.EVENTNAME_CREATE, taskNotificationListener);
    }
}