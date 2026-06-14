package com.adas.retrofit.dto;

/** Тело формы «Первичная приёмка»: пробег, госномер, номер СТС + id завершаемой задачи. */
public record AcceptanceRequest(
        Integer mileage,
        String licensePlate,
        String stsNumber,
        String taskId
) {
}