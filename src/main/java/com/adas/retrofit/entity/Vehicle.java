package com.adas.retrofit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Автомобиль, поступивший на дооснащение. */
@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String vin;

    private String model;

    @Column(name = "production_year")
    private Integer year;

    /** Комплектация. */
    private String trimLevel;

    /** Госномер (фиксируется на первичной приёмке). */
    private String licensePlate;

    /** Номер СТС (фиксируется на первичной приёмке). */
    private String stsNumber;
}