package com.adas.retrofit.delegate;

import com.adas.retrofit.entity.Order;
import com.adas.retrofit.service.CustomerReportService;
import com.adas.retrofit.service.OrderService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Execution listener на завершающем событии Event_Success: переводит заявку в COMPLETED
 * и отправляет клиенту итоговый отчёт по дооснащению на e-mail.
 */
@Component("completeOrderListener")
public class CompleteOrderListener implements ExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(CompleteOrderListener.class);

    private final OrderService orderService;
    private final CustomerReportService customerReportService;

    public CompleteOrderListener(OrderService orderService, CustomerReportService customerReportService) {
        this.orderService = orderService;
        this.customerReportService = customerReportService;
    }

    @Override
    public void notify(DelegateExecution execution) {
        UUID orderId = UUID.fromString((String) execution.getVariable(ProcessVariables.ORDER_ID));
        Order order = orderService.markCompleted(orderId);
        // Исход «демонтаж/возврат»: система снята, авто возвращено в исходное состояние.
        // CODING и перемонтаж — успешные исходы, отчёт по ним обычный.
        boolean dismantled = "DISMANTLE".equals(execution.getVariable(ProcessVariables.FAULT_TYPE));
        log.info("Процесс завершён, заявка {} переведена в COMPLETED (демонтаж={})", orderId, dismantled);
        customerReportService.sendCompletionReport(order, dismantled);
    }
}