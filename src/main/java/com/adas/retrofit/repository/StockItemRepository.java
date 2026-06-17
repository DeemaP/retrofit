package com.adas.retrofit.repository;

import com.adas.retrofit.entity.StockItem;
import com.adas.retrofit.entity.StockType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockItemRepository extends JpaRepository<StockItem, UUID> {

    Optional<StockItem> findByTypeAndSku(StockType type, String sku);

    boolean existsByTypeAndSku(StockType type, String sku);
}