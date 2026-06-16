package com.adas.retrofit.repository;

import com.adas.retrofit.entity.CalibrationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CalibrationRecordRepository extends JpaRepository<CalibrationRecord, UUID> {

    List<CalibrationRecord> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}