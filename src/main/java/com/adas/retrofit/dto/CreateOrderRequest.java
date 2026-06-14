package com.adas.retrofit.dto;

import com.adas.retrofit.entity.RetrofitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Запрос на создание заявки. Клиент и автомобиль создаются на лету
 * (для альфы — без поиска существующих).
 */
public record CreateOrderRequest(
        @NotBlank String customerFullName,
        String customerPhone,
        String customerEmail,
        @NotBlank String vehicleVin,
        String vehicleModel,
        Integer vehicleYear,
        String vehicleTrimLevel,
        @NotNull RetrofitType retrofitType
) {
}