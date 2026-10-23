package com.openlab.qualitos.quality.spc.gagerr.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "spc_gage_rr_studies")
@Getter
@Setter
@NoArgsConstructor
public class GageRrStudyJpaEntity {

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
    private String instrument;

    @Column(nullable = false)
    private double lsl;

    @Column(nullable = false)
    private double usl;

    @Column(name = "operator_count", nullable = false)
    private int operatorCount;

    @Column(name = "part_count", nullable = false)
    private int partCount;

    @Column(name = "trial_count", nullable = false)
    private int trialCount;

    @Column(name = "sigma_multiplier", nullable = false)
    private double sigmaMultiplier;

    @Column(name = "study_date", nullable = false)
    private LocalDate studyDate;

    /** Le compte du responsable de l'étude, choisi dans l'annuaire. */
    @Column(name = "responsible_user_id")
    private UUID responsibleUserId;

    /** Son nom, recopié pour l'affichage. */
    @Column(name = "responsible_name", length = 150)
    private String responsibleName;

    /** Les opérateurs (compte et nom) en JSON, dans l'ordre des blocs de la grille. */
    @Column(nullable = false, columnDefinition = "text")
    private String operators;

    /** La grille [opérateur][pièce][essai] en JSON : lue et écrite d'un bloc. */
    @Column(nullable = false, columnDefinition = "text")
    private String measurements;

    @Column(name = "grr_percent", nullable = false)
    private double grrPercent;

    @Column(name = "grr_tolerance_percent", nullable = false)
    private double grrTolerancePercent;

    @Column(nullable = false)
    private int ndc;

    @Column(nullable = false, length = 20)
    private String decision;

    @Column(name = "anova_grr_percent", nullable = false)
    private double anovaGrrPercent;

    @Column(name = "anova_ndc", nullable = false)
    private int anovaNdc;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
