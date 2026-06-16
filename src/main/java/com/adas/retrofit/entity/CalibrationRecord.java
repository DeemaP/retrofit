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
 * Запись о калибровке дооснащённой системы (статическая/динамическая).
 * Создаётся на ЮТ «Кодирование и калибровка».
 */
@Entity
@Table(name = "calibration_records")
@Getter
@Setter
@NoArgsConstructor
public class CalibrationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalibrationType type = CalibrationType.STATIC;

    /** Параметры калибровки (для альфы — TEXT, портируемо на H2/Postgres). */
    @Column(columnDefinition = "TEXT")
    private String parameters;

    /** Пройдена ли калибровка успешно. */
    private boolean passed;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}