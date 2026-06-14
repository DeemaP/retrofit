package com.adas.retrofit.delegate;

import com.adas.retrofit.service.OrderService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Execution listener на завершающем событии Event_Success: переводит заявку в COMPLETED.
 */
@Component("completeOrderListener")
public class CompleteOrderListener implements ExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(CompleteOrderListener.class);

    private final OrderService orderService;

    public CompleteOrderListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void notify(DelegateExecution execution) {
        UUID orderId = UUID.fromString((String) execution.getVariable(ProcessVariables.ORDER_ID));
        orderService.markCompleted(orderId);
        log.info("Процесс завершён успешно, заявка {} переведена в COMPLETED", orderId);
    }
}