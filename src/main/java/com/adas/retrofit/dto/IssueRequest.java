package com.adas.retrofit.dto;

import java.util.List;
import java.util.UUID;

/**
 * Выдача исполнителям: id выдаваемых запчастей и оборудования (пустые списки = выдать всё)
 * + id завершаемой задачи.
 */
public record IssueRequest(
        List<UUID> partIds,
        List<UUID> equipmentIds,
        String taskId
) {
}