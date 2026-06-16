package com.adas.retrofit.repository;

import com.adas.retrofit.entity.ProgrammingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProgrammingRecordRepository extends JpaRepository<ProgrammingRecord, UUID> {

    List<ProgrammingRecord> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}