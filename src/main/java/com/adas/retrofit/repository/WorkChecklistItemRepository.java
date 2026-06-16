package com.adas.retrofit.repository;

import com.adas.retrofit.entity.WorkChecklistItem;
import com.adas.retrofit.entity.WorkPhase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkChecklistItemRepository extends JpaRepository<WorkChecklistItem, UUID> {

    List<WorkChecklistItem> findByOrderIdAndPhaseOrderByPositionAsc(UUID orderId, WorkPhase phase);

    void deleteByOrderIdAndPhase(UUID orderId, WorkPhase phase);
}