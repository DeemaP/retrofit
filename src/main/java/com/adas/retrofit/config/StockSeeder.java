package com.adas.retrofit.config;

import com.adas.retrofit.entity.RetrofitType;
import com.adas.retrofit.entity.StockItem;
import com.adas.retrofit.entity.StockType;
import com.adas.retrofit.repository.StockItemRepository;
import com.adas.retrofit.service.RetrofitCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Засев склада начальными остатками по всем позициям справочника {@link RetrofitCatalog}.
 * Идемпотентно: создаёт только отсутствующие позиции, существующие остатки не трогает.
 * Ключ позиции (sku) для каталога = наименование (артикулов в каталоге нет).
 */
@Component
public class StockSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StockSeeder.class);

    private final StockItemRepository stockRepository;
    private final int initialQuantity;

    public StockSeeder(StockItemRepository stockRepository,
                       @Value("${stock.initial-quantity:10}") int initialQuantity) {
        this.stockRepository = stockRepository;
        this.initialQuantity = initialQuantity;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int created = 0;
        Set<String> partNames = new LinkedHashSet<>();
        Set<String> equipmentNames = new LinkedHashSet<>();
        for (RetrofitType type : RetrofitType.values()) {
            partNames.addAll(RetrofitCatalog.partsFor(type));
            equipmentNames.addAll(RetrofitCatalog.equipmentFor(type));
        }
        created += seed(StockType.PART, partNames);
        created += seed(StockType.EQUIPMENT, equipmentNames);
        if (created > 0) {
            log.info("Склад: засеяно {} новых позиций (по {} шт.)", created, initialQuantity);
        }
    }

    private int seed(StockType type, Set<String> names) {
        int created = 0;
        for (String name : names) {
            if (!stockRepository.existsByTypeAndSku(type, name)) {
                StockItem item = new StockItem();
                item.setType(type);
                item.setSku(name);
                item.setName(name);
                item.setQuantityOnHand(initialQuantity);
                stockRepository.save(item);
                created++;
            }
        }
        return created;
    }
}