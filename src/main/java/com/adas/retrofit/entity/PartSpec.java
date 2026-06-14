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
 * Справочная позиция требуемых <b>запчастей</b> для пары «модель + год выпуска + тип дооснащения».
 * Заполняется на этапе формирования списка и переиспользуется для последующих заявок той же
 * конфигурации (этап «Проверка БД на наличие списка»). Не путать с {@link Part} — та привязана
 * к конкретной заявке.
 */
@Entity
@Table(name = "part_specs", indexes = {
        @Index(name = "idx_part_spec_key", columnList = "model,production_year,retrofit_type")
})
@Getter
@Setter
@NoArgsConstructor
public class PartSpec {

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

    private int quantity = 1;
}