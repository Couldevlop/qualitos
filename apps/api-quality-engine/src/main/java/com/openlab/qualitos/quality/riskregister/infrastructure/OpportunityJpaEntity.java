package com.openlab.qualitos.quality.riskregister.infrastructure;

import com.openlab.qualitos.quality.riskregister.domain.OpportunityDecision;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityStatus;
import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;
import com.openlab.qualitos.quality.riskregister.domain.RegisterType;
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
@Table(name = "risk_register_opportunities")
@Getter
@Setter
@NoArgsConstructor
public class OpportunityJpaEntity {

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

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(length = 4000)
    private String context;

    @Column(length = 4000)
    private String benefit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RegisterOrigin origin;

    @Column(name = "origin_ref", length = 120)
    private String originRef;

    @Column(nullable = false)
    private int gain;

    @Column(nullable = false)
    private int feasibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OpportunityDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OpportunityStatus status;

    @Column(nullable = false, length = 255)
    private String requirements;

    @Column(name = "benefit_criterion", length = 1000)
    private String benefitCriterion;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
