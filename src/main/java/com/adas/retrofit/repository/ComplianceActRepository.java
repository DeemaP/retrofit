package com.adas.retrofit.repository;

import com.adas.retrofit.entity.ComplianceAct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ComplianceActRepository extends JpaRepository<ComplianceAct, UUID> {

    Optional<ComplianceAct> findByOrderId(UUID orderId);
}