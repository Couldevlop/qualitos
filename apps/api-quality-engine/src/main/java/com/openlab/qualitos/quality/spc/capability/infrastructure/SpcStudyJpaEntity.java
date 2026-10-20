package com.openlab.qualitos.quality.spc.capability.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "spc_capability_studies")
@Getter
@Setter
@NoArgsConstructor
public class SpcStudyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "part_reference", nullable = false, length = 120)
    private String partReference;

    @Column(length = 150)
    private String characteristic;

    @Column(length = 150)
    private String specification;

    @Column(length = 80)
    private String machine;

    /** Le compte du meneur de l'étude, choisi dans l'annuaire. */
    @Column(name = "leader_user_id")
    private UUID leaderUserId;

    /** Son nom, recopié pour l'affichage. */
    @Column(name = "leader_name", length = 150)
    private String leaderName;

    @Column(name = "study_date", nullable = false)
    private LocalDate studyDate;

    @Column(name = "action_plan", nullable = false)
    private boolean actionPlan;

    @Column(nullable = false)
    private double lsl;

    @Column(nullable = false)
    private double usl;

    @Column(name = "subgroup_size", nullable = false)
    private int subgroupSize;

    /** La grille en JSON : lue et écrite d'un bloc, jamais interrogée par le SGBD. */
    @Column(nullable = false, columnDefinition = "text")
    private String measurements;

    @Column(nullable = false)
    private double cp;

    @Column(nullable = false)
    private double cpk;

    @Column(name = "capable_threshold", nullable = false, precision = 4, scale = 2, updatable = false)
    private BigDecimal capableThreshold;

    @Column(name = "watch_threshold", nullable = false, precision = 4, scale = 2, updatable = false)
    private BigDecimal watchThreshold;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
