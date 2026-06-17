package com.adas.retrofit.controller;

import com.adas.retrofit.dto.CreateOrderRequest;
import com.adas.retrofit.dto.OrderResponse;
import com.adas.retrofit.dto.SupplyResponse;
import com.adas.retrofit.entity.Order;
import com.adas.retrofit.entity.StockType;
import com.adas.retrofit.service.OrderService;
import com.adas.retrofit.service.SupplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Заявки на дооснащение ADAS")
public class OrderController {

    private final OrderService orderService;
    private final SupplyService supplyService;

    public OrderController(OrderService orderService, SupplyService supplyService) {
        this.orderService = orderService;
        this.supplyService = supplyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать заявку и запустить процесс дооснащения")
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(request);
        return OrderResponse.of(order);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить заявку с текущим статусом и активными задачами")
    public ResponseEntity<OrderResponse> get(@PathVariable UUID id) {
        Order order = orderService.getOrder(id);
        return ResponseEntity.ok(OrderResponse.of(order, orderService.listActiveTasks(id)));
    }

    @GetMapping("/{id}/supply")
    @Operation(summary = "Списки снабжения заявки: запчасти и оборудование")
    public SupplyResponse supply(@PathVariable UUID id) {
        orderService.getOrder(id); // 404, если заявки нет
        return SupplyResponse.of(
                supplyService.partsOf(id), supplyService.equipmentOf(id),
                p -> supplyService.stockOnHand(StockType.PART, p.getArticle(), p.getName()),
                e -> supplyService.stockOnHand(StockType.EQUIPMENT, e.getArticle(), e.getName()));
    }
}