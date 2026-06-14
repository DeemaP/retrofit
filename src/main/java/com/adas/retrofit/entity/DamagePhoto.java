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
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Фотография повреждения, сделанная на этапе фотофиксации и привязанная к заявке.
 * Сами байты лежат в объектном хранилище MinIO (ключ — {@link #objectKey}),
 * в БД хранятся только метаданные.
 */
@Entity
@Table(name = "damage_photos")
@Getter
@Setter
@NoArgsConstructor
public class DamagePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    /** Ключ объекта в бакете MinIO. */
    @Column(nullable = false)
    private String objectKey;

    /** Исходное имя файла. */
    private String originalFilename;

    private String contentType;

    private long size;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant uploadedAt;
}