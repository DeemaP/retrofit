package com.adas.retrofit.service;

import com.adas.retrofit.dto.CreateOrderRequest;
import com.adas.retrofit.entity.Order;
import com.adas.retrofit.entity.Part;
import com.adas.retrofit.entity.StockType;
import com.adas.retrofit.entity.SupplyOrderStatus;
import com.adas.retrofit.repository.PartRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Количественный учёт склада и по-позиционные этапы заказа недостающего:
 * проверка по количеству → дефицит → заказано → поступило (с пополнением) → выдача (списание).
 */
@SpringBootTest
class SupplyStockTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private SpecCatalogService specCatalogService;
    @Autowired
    private SupplyService supplyService;
    @Autowired
    private PartRepository partRepository;

    @Test
    void quantityDrivenStockAndPerItemOrdering() {
        Order order = orderService.createOrder(new CreateOrderRequest(
                "Пётр Тест", "+70000000001", "petr@example.com",
                "WVWZZZ1JZXW000777", "VW Passat", 2020, "Highline",
                com.adas.retrofit.entity.RetrofitType.ACC));
        UUID orderId = order.getId();

        // Разворачиваем список запчастей под заявку (имена из каталога, qty=1)
        List<Part> parts = specCatalogService.materializeParts(orderId);
        assertThat(parts).isNotEmpty();

        // Склад засеян (>=1 каждой позиции) — наличие достаточно
        assertThat(supplyService.checkPartsStock(orderId, false)).isTrue();
        assertThat(partRepository.findByOrderId(orderId)).allMatch(Part::isInStock);

        // Обнуляем остаток одной позиции → дефицит именно по количеству
        Part target = parts.get(0);
        String sku = target.getArticle() != null && !target.getArticle().isBlank()
                ? target.getArticle() : target.getName();
        supplyService.setStock(StockType.PART, sku, target.getName(), 0);

        assertThat(supplyService.checkPartsStock(orderId, false)).isFalse();
        assertThat(supplyService.missingParts(orderId)).extracting(Part::getId).contains(target.getId());
        assertThat(supplyService.orderStatus(orderId)).isEqualTo(SupplyOrderStatus.TO_ORDER);

        // Этап «заказано»
        supplyService.markOrdered(orderId, Set.of(target.getId()), Set.of());
        assertThat(supplyService.orderStatus(orderId)).isEqualTo(SupplyOrderStatus.ORDERED);
        assertThat(supplyService.allReceived(orderId)).isFalse();

        // Этап «поступило» — позиция в наличии, склад пополнен
        supplyService.markReceived(orderId, Set.of(target.getId()), Set.of());
        assertThat(supplyService.allReceived(orderId)).isTrue();
        assertThat(supplyService.orderStatus(orderId)).isEqualTo(SupplyOrderStatus.RECEIVED);
        assertThat(supplyService.stockOnHand(StockType.PART, sku, target.getName()))
                .isGreaterThanOrEqualTo(target.getQuantity());

        // Выдача списывает остаток со склада
        int before = supplyService.stockOnHand(StockType.PART, sku, target.getName());
        supplyService.issue(orderId, Set.of(target.getId()), Set.of());
        assertThat(supplyService.stockOnHand(StockType.PART, sku, target.getName()))
                .isEqualTo(before - target.getQuantity());
        assertThat(partRepository.findById(target.getId())).get()
                .matches(Part::isIssued);
    }
}