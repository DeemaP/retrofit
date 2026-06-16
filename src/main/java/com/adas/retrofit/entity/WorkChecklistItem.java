package com.adas.retrofit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Пункт чек-листа слесарно-монтажных работ (разборка, монтаж/демонтаж оборудования, сборка,
 * проверка корректности). Привязан к заявке и фазе работ ({@link WorkPhase}).
 */
@Entity
@Table(name = "work_checklist_items")
@Getter
@Setter
@NoArgsConstructor
public class WorkChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkPhase phase;

    @Column(nullable = false)
    private String title;

    /** Выполнен ли пункт. */
    private boolean done;

    /** Порядок отображения в чек-листе. */
    private int position;
}