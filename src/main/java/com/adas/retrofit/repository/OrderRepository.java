package com.adas.retrofit.repository;

import com.adas.retrofit.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByProcessInstanceId(String processInstanceId);
}