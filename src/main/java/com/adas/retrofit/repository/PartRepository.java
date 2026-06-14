package com.adas.retrofit.repository;

import com.adas.retrofit.entity.Part;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartRepository extends JpaRepository<Part, UUID> {

    List<Part> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    void deleteByOrderId(UUID orderId);
}