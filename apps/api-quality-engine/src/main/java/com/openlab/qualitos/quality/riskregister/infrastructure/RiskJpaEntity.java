package com.openlab.qualitos.quality.riskregister.infrastructure;

import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;
import com.openlab.qualitos.quality.riskregister.domain.RegisterType;
import com.openlab.qualitos.quality.riskregister.domain.RiskDecision;
import com.openlab.qualitos.quality.riskregister.domain.RiskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "risk_register_risks")
@Getter
@Setter
@NoArgsConstructor
public class RiskJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 20, updatable = false)
    private String reference;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RegisterType type;

    @Column(nullable = false, length = 120)
    private String process;

    @Column(length = 120)
    private String site;

    @Column(nullable = false, length = 150)
    private String owner;

    @Column(length = 4000)
    private String cause;

    @Column(length = 4000)
    private String effect;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RegisterOrigin origin;

    @Column(name = "origin_ref", length = 120)
    private String originRef;

    @Column(name = "source_id", updatable = false)
    private UUID sourceId;

    @Column(name = "gross_severity", nullable = false)
    private int grossSeverity;

    @Column(name = "gross_probability", nullable = false)
    private int grossProbability;

    @Column(name = "residual_severity")
    private Integer residualSeverity;

    @Column(name = "residual_probability")
    private Integer residualProbability;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RiskDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RiskStatus status;

    /** Codes d'exigence séparés par des virgules : une colonne, pas de N+1 sur la liste. */
    @Column(nullable = false, length = 255)
    private String requirements;

    @Column(name = "next_review_on")
    private LocalDate nextReviewOn;

    @Column(name = "effectiveness_criterion", length = 1000)
    private String effectivenessCriterion;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
