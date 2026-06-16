package com.adas.retrofit.process;

import com.adas.retrofit.delegate.ProcessVariables;
import com.adas.retrofit.dto.CreateOrderRequest;
import com.adas.retrofit.entity.Order;
import com.adas.retrofit.entity.RetrofitType;
import com.adas.retrofit.service.OrderService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест ветки исключения: «Проверка системы и тест-драйв» завершается с
 * {@code testDrivePassed = false}. Проверяет, что новый шлюз Gateway_1n2411l
 * по условию {@code ${testDrivePassed == false}} направляет процесс на
 * «Выявление причины неисправности» (Activity_0kgksar), а не на выдачу документов.
 */
@SpringBootTest
class ProcessTestDriveFailedTest {

    private static final int MAX_STEPS = 50;
    private static final String TEST_DRIVE = "Activity_0p38zs6";
    private static final String FAULT_CAUSE = "Activity_0kgksar";

    @Autowired
    private OrderService orderService;
    @Autowired
    private TaskService taskService;

    @Test
    void failedTestDriveRoutesToFaultCause() {
        Order order = orderService.createOrder(new CreateOrderRequest(
                "Пётр Петров", "+70000000001", "petr@example.com",
                "WVWZZZ1JZXW000002", "VW Passat", 2020, "Highline",
                RetrofitType.LKA));
        String pid = order.getProcessInstanceId();
        assertThat(pid).isNotNull();

        // Гоним процесс по happy-path до тест-драйва; тест-драйв заваливаем.
        boolean failedTestDrive = false;
        for (int step = 0; step < MAX_STEPS && !failedTestDrive; step++) {
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(pid).active().list();
            if (tasks.isEmpty()) {
                break;
            }
            for (Task task : tasks) {
                if (TEST_DRIVE.equals(task.getTaskDefinitionKey())) {
                    taskService.complete(task.getId(),
                            Map.of(ProcessVariables.TEST_DRIVE_PASSED, false));
                    failedTestDrive = true;
                    break;
                }
                taskService.complete(task.getId(), Map.of());
            }
        }

        assertThat(failedTestDrive)
                .as("тест-драйв должен был встретиться в процессе")
                .isTrue();

        // После провала тест-драйва активна задача «Выявление причины неисправности».
        List<Task> after = taskService.createTaskQuery()
                .processInstanceId(pid).active().list();
        assertThat(after)
                .extracting(Task::getTaskDefinitionKey)
                .contains(FAULT_CAUSE);
    }
}