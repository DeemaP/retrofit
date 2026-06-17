package com.adas.retrofit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Запчасть — компонент, который <b>устанавливается в автомобиль</b> при дооснащении
 * (радар, датчик, кронштейн, разъём, жгут). Список формируется под конкретную заявку.
 * Не путать с {@link Equipment} (оснастка цеха).
 */
@Entity
@Table(name = "parts")
@Getter
@Setter
@NoArgsConstructor
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private String name;

    /** Артикул (каталожный номер) запчасти. */
    private String article;

    private int quantity = 1;

    /** Достаточно ли на складе под потребность (результат проверки наличия). */
    private boolean inStock;

    /** Отмечена заказанной (этап «Заказ недостающих…», подсостояние «заказано»). */
    private boolean ordered;

    /** Выдана исполнителю и списана со склада (этап выдачи). */
    private boolean issued;
}