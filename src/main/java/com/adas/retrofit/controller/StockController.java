package com.adas.retrofit.controller;

import com.adas.retrofit.dto.SetStockRequest;
import com.adas.retrofit.dto.StockItemView;
import com.adas.retrofit.service.SupplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Складские остатки: просмотр и корректировка количества. */
@RestController
@RequestMapping("/api/v1/stock")
@Tag(name = "Stock", description = "Складские остатки (количественный учёт)")
public class StockController {

    private final SupplyService supplyService;

    public StockController(SupplyService supplyService) {
        this.supplyService = supplyService;
    }

    @GetMapping
    @Operation(summary = "Текущие остатки на складе")
    public List<StockItemView> list() {
        return supplyService.listStock().stream().map(StockItemView::of).toList();
    }

    @PostMapping
    @Operation(summary = "Установить остаток позиции (создаёт позицию при отсутствии)")
    public StockItemView set(@Valid @RequestBody SetStockRequest req) {
        return StockItemView.of(
                supplyService.setStock(req.type(), req.sku(), req.name(), req.quantityOnHand()));
    }
}