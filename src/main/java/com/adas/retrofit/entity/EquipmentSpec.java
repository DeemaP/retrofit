package com.adas.retrofit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Справочная позиция требуемого <b>оборудования</b> для пары «модель + год выпуска + тип дооснащения».
 * Аналог {@link PartSpec} для оснастки цеха. Не путать с {@link Equipment} (привязана к заявке).
 */
@Entity
@Table(name = "equipment_specs", indexes = {
        @Index(name = "idx_equipment_spec_key", columnList = "model,production_year,retrofit_type")
})
@Getter
@Setter
@NoArgsConstructor
public class EquipmentSpec {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String model;

    @Column(name = "production_year")
    private Integer productionYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "retrofit_type", nullable = false)
    private RetrofitType retrofitType;

    @Column(nullable = false)
    private String name;

    private String article;
}