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
 * Оборудование — оснастка, которой <b>выполняют дооснащение</b> (диагностический компьютер
 * с ПО, специнструмент, калибровочный стенд, мишени). Список требуемого оборудования
 * формируется под конкретную заявку. Не путать с {@link Part} (ставится в авто).
 */
@Entity
@Table(name = "equipment")
@Getter
@Setter
@NoArgsConstructor
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private String name;

    /** Артикул (каталожный номер) оборудования. */
    private String article;

    /** Доступность оснастки (результат проверки наличия). */
    private boolean inStock;

    /** Выдана исполнителю и списана со склада (этап выдачи). */
    private boolean issued;
}