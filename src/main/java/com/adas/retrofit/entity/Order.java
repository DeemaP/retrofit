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
 * Заявка на дооснащение. С каждой заявкой ассоциирован один экземпляр
 * процесса Camunda (см. {@link #processInstanceId}).
 */
@Entity
@Table(name = "orders") // order — зарезервированное слово SQL
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RetrofitType retrofitType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.DRAFT;

    /** Пробег на момент приёмки, км (фиксируется на первичной приёмке). */
    private Integer mileage;

    /** ТЗ/пожелания и список жалоб клиента (фиксация ТЗ). */
    @Column(columnDefinition = "TEXT")
    private String clientRequirements;

    /** Причина, если дооснащение невозможно (первичная диагностика). */
    @Column(columnDefinition = "TEXT")
    private String feasibilityComment;

    /** Статус заказа недостающего (ЮТ «Заказ недостающих запчастей и оборудования»). */
    @Enumerated(EnumType.STRING)
    private SupplyOrderStatus supplyOrderStatus;

    /** id экземпляра процесса Camunda, запущенного для этой заявки. */
    private String processInstanceId;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}