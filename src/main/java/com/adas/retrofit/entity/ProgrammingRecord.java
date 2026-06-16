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
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Запись о программировании ЭБУ: при дооснащении ({@link ProgrammingDirection#INSTALL})
 * либо при возврате авто в исходное состояние ({@link ProgrammingDirection#RESTORE}).
 * Создаётся на ЮТ «Кодирование и калибровка» и «Возврат авто в изначальное состояние».
 */
@Entity
@Table(name = "programming_records")
@Getter
@Setter
@NoArgsConstructor
public class ProgrammingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    /** Наименование/адрес ЭБУ (например, «Камера ADAS», «Радар переднего обзора»). */
    @Column(nullable = false)
    private String ecuName;

    /** Версия ПО до перепрошивки. */
    private String swBefore;

    /** Версия ПО после перепрошивки. */
    private String swAfter;

    /** Активированные функции (для альфы — TEXT, портируемо на H2/Postgres). */
    @Column(columnDefinition = "TEXT")
    private String activatedFeatures;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgrammingDirection direction = ProgrammingDirection.INSTALL;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}