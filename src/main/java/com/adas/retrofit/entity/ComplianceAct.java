package com.adas.retrofit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Акт о соответствии / комплект документации, выдаваемый клиенту по завершении
 * дооснащения. Создаётся делегатом GenerateDocumentationDelegate.
 */
@Entity
@Table(name = "compliance_acts")
@Getter
@Setter
@NoArgsConstructor
public class ComplianceAct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "order_id", unique = true)
    private Order order;

    @Column(nullable = false)
    private String documentNumber;

    private Instant issuedAt;

    /** Сводка по проекту. Для альфы — TEXT (портируемо на H2/Postgres). */
    @Column(columnDefinition = "TEXT")
    private String summary;
}