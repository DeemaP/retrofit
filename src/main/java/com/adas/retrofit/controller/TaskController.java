package com.adas.retrofit.controller;

import com.adas.retrofit.dto.TaskView;
import com.adas.retrofit.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.camunda.bpm.engine.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks", description = "User task процесса дооснащения")
public class TaskController {

    private final OrderService orderService;
    private final TaskService taskService;

    public TaskController(OrderService orderService, TaskService taskService) {
        this.orderService = orderService;
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "Список активных задач по заявке")
    public List<TaskView> list(@RequestParam UUID orderId) {
        return orderService.listActiveTasks(orderId);
    }

    @PostMapping("/{taskId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Завершить задачу (тело — произвольные переменные процесса)")
    public void complete(@PathVariable String taskId,
                         @RequestBody(required = false) Map<String, Object> variables) {
        taskService.complete(taskId, variables != null ? variables : Map.of());
    }
}