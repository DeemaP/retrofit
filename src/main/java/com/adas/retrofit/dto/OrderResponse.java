package com.adas.retrofit.dto;

import com.adas.retrofit.entity.Order;
import com.adas.retrofit.entity.OrderStatus;
import com.adas.retrofit.entity.RetrofitType;
import com.adas.retrofit.entity.SupplyOrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ответ по заявке. {@code activeTasks} заполняется только в GET /orders/{id};
 * в ответе на создание он пустой.
 */
public record OrderResponse(
        UUID id,
        OrderStatus status,
        RetrofitType retrofitType,
        String processInstanceId,
        String customerFullName,
        String vehicleVin,
        String vehicleModel,
        String licensePlate,
        String stsNumber,
        Integer mileage,
        String clientRequirements,
        String feasibilityComment,
        SupplyOrderStatus supplyOrderStatus,
        Instant createdAt,
        List<TaskView> activeTasks
) {

    public static OrderResponse of(Order order, List<TaskView> activeTasks) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getRetrofitType(),
                order.getProcessInstanceId(),
                order.getCustomer() != null ? order.getCustomer().getFullName() : null,
                order.getVehicle() != null ? order.getVehicle().getVin() : null,
                order.getVehicle() != null ? order.getVehicle().getModel() : null,
                order.getVehicle() != null ? order.getVehicle().getLicensePlate() : null,
                order.getVehicle() != null ? order.getVehicle().getStsNumber() : null,
                order.getMileage(),
                order.getClientRequirements(),
                order.getFeasibilityComment(),
                order.getSupplyOrderStatus(),
                order.getCreatedAt(),
                activeTasks
        );
    }

    public static OrderResponse of(Order order) {
        return of(order, List.of());
    }
}