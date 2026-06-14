package com.adas.retrofit.process;

import com.adas.retrofit.dto.CreateOrderRequest;
import com.adas.retrofit.entity.Order;
import com.adas.retrofit.entity.OrderStatus;
import com.adas.retrofit.entity.RetrofitType;
import com.adas.retrofit.repository.ComplianceActRepository;
import com.adas.retrofit.repository.EquipmentRepository;
import com.adas.retrofit.repository.OrderRepository;
import com.adas.retrofit.repository.PartRepository;
import com.adas.retrofit.service.OrderService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест процесса (использует Spring-managed движок Camunda и camunda-bpm-junit5 в classpath):
 * стартует процесс через OrderService, проходит happy-path, завершая все активные user task
 * с пустыми переменными (гейтвеи настроены на default-успех), и проверяет, что процесс
 * достиг Event_Success — заявка переведена в COMPLETED и создан ComplianceAct.
 */
@SpringBootTest
class ProcessHappyPathTest {

    /** Защита от зацикливания: на happy-path задач заведомо меньше. */
    private static final int MAX_STEPS = 50;

    @Autowired
    private OrderService orderService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ComplianceActRepository actRepository;
    @Autowired
    private PartRepository partRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;

    @Test
    void happyPathReachesSuccessAndCreatesAct() {
        Order order = orderService.createOrder(new CreateOrderRequest(
                "Иван Иванов", "+70000000000", "ivan@example.com",
                "WVWZZZ1JZXW000001", "VW Golf", 2019, "Comfortline",
                RetrofitType.ACC));

        String processInstanceId = order.getProcessInstanceId();
        assertThat(processInstanceId).isNotNull();

        completeAllUserTasks(processInstanceId);

        // Процесс завершён (нет активного экземпляра)
        long active = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).count();
        assertThat(active).isZero();

        // Заявка переведена в COMPLETED листенером Event_Success
        Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        // Создан акт о соответствии (GenerateDocumentationDelegate)
        assertThat(actRepository.findByOrderId(order.getId())).isPresent();

        // Сохранены раздельные списки запчастей и оборудования, проверено наличие
        assertThat(partRepository.findByOrderId(order.getId()))
                .isNotEmpty()
                .allMatch(p -> p.isInStock());
        assertThat(equipmentRepository.findByOrderId(order.getId()))
                .isNotEmpty()
                .allMatch(e -> e.isInStock());
    }

    /** Завершает активные user task пакет за пакетом, пока процесс не дойдёт до конца. */
    private void completeAllUserTasks(String processInstanceId) {
        for (int step = 0; step < MAX_STEPS; step++) {
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .list();
            if (tasks.isEmpty()) {
                return;
            }
            for (Task task : tasks) {
                taskService.complete(task.getId(), Map.of());
            }
        }
        throw new IllegalStateException("Процесс не завершился за " + MAX_STEPS + " шагов");
    }
}