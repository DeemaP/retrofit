package com.adas.retrofit.service;

import com.adas.retrofit.delegate.ProcessVariables;
import com.adas.retrofit.dto.CreateOrderRequest;
import com.adas.retrofit.dto.TaskView;
import com.adas.retrofit.entity.Customer;
import com.adas.retrofit.entity.Order;
import com.adas.retrofit.entity.OrderStatus;
import com.adas.retrofit.entity.Vehicle;
import com.adas.retrofit.repository.CustomerRepository;
import com.adas.retrofit.repository.OrderRepository;
import com.adas.retrofit.repository.VehicleRepository;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Создание заявок и взаимодействие с движком процессов. */
@Service
public class OrderService {

    public static final String PROCESS_KEY = "Process_1p16tp4";

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        VehicleRepository vehicleRepository,
                        RuntimeService runtimeService,
                        TaskService taskService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    /** Создаёт клиента/авто/заявку и запускает процесс Camunda. */
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Customer customer = new Customer();
        customer.setFullName(request.customerFullName());
        customer.setPhone(request.customerPhone());
        customer.setEmail(request.customerEmail());
        customer = customerRepository.save(customer);

        Vehicle vehicle = new Vehicle();
        vehicle.setVin(request.vehicleVin());
        vehicle.setModel(request.vehicleModel());
        vehicle.setYear(request.vehicleYear());
        vehicle.setTrimLevel(request.vehicleTrimLevel());
        vehicle = vehicleRepository.save(vehicle);

        Order order = new Order();
        order.setCustomer(customer);
        order.setVehicle(vehicle);
        order.setRetrofitType(request.retrofitType());
        order.setStatus(OrderStatus.DRAFT);
        order = orderRepository.save(order);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                PROCESS_KEY,
                order.getId().toString(),
                ProcessVariables.initialVariables(order.getId().toString()));

        order.setProcessInstanceId(instance.getId());
        order.setStatus(OrderStatus.IN_PROGRESS);
        order = orderRepository.save(order);

        log.info("Создана заявка {} и запущен процесс {}", order.getId(), instance.getId());
        return order;
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /** Активные user task заявки (через Camunda TaskService по processInstanceId). */
    @Transactional(readOnly = true)
    public List<TaskView> listActiveTasks(UUID orderId) {
        Order order = getOrder(orderId);
        if (order.getProcessInstanceId() == null) {
            return List.of();
        }
        return taskService.createTaskQuery()
                .processInstanceId(order.getProcessInstanceId())
                .active()
                .orderByTaskCreateTime().asc()
                .list().stream()
                .map(OrderService::toTaskView)
                .toList();
    }

    /** Первичная приёмка: пробег, госномер и номер СТС. */
    @Transactional
    public Order saveAcceptance(UUID orderId, Integer mileage, String licensePlate, String stsNumber) {
        Order order = getOrder(orderId);
        order.setMileage(mileage);
        Vehicle vehicle = order.getVehicle();
        if (vehicle != null) {
            vehicle.setLicensePlate(licensePlate);
            vehicle.setStsNumber(stsNumber);
            vehicleRepository.save(vehicle);
        }
        return orderRepository.save(order);
    }

    /** Фиксация ТЗ/пожеланий и жалоб клиента. */
    @Transactional
    public Order saveRequirements(UUID orderId, String requirements) {
        Order order = getOrder(orderId);
        order.setClientRequirements(requirements);
        return orderRepository.save(order);
    }

    /** Результат первичной диагностики: причину сохраняем, если дооснащение невозможно. */
    @Transactional
    public Order saveFeasibility(UUID orderId, boolean possible, String comment) {
        Order order = getOrder(orderId);
        order.setFeasibilityComment(possible ? null : comment);
        return orderRepository.save(order);
    }

    /** Переводит заявку в COMPLETED (вызывается листенером Event_Success). */
    @Transactional
    public void markCompleted(UUID orderId) {
        Order order = getOrder(orderId);
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
    }

    static TaskView toTaskView(Task task) {
        return new TaskView(
                task.getId(),
                task.getName(),
                task.getAssignee(),
                null,
                task.getCreateTime(),
                task.getTaskDefinitionKey());
    }
}