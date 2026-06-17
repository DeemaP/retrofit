package com.adas.retrofit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Складская позиция (остаток на складе). В отличие от {@link Part}/{@link Equipment},
 * которые привязаны к конкретной заявке, StockItem — общий учёт количества на складе,
 * ключ — {@code type + sku} (sku = артикул, а при его отсутствии — наименование).
 *
 * <p>Проверка наличия сравнивает потребность заявки с {@link #quantityOnHand};
 * выдача исполнителям списывает остаток, поступление по заказу — пополняет.
 */
@Entity
@Table(name = "stock_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_stock_type_sku", columnNames = {"type", "sku"}))
@Getter
@Setter
@NoArgsConstructor
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockType type;

    /** Ключ позиции: артикул, либо наименование, если артикула нет. */
    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    /** Количество на складе. */
    @Column(nullable = false)
    private int quantityOnHand;
}