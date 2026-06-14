package com.adas.retrofit.repository;

import com.adas.retrofit.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {

    List<Equipment> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    void deleteByOrderId(UUID orderId);
}