package com.openlab.qualitos.quality.objective;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Un objectif qualité mesurable (ISO 9001 §6.2) : ce qu'on attend d'un KPI, et
 * la règle qui propose une CAPA quand l'attente n'est pas tenue.
 *
 * <p><b>Ce n'est pas un KPI de plus.</b> L'indicateur constate ; l'objectif
 * engage. Un même KPI sert plusieurs objectifs — le taux de rebut visé à 1,5 %
 * en production et à 0,8 % sur la ligne neuve — et chaque objectif a son
 * processus, sa clause, son pilote et sa règle de déclenchement. Les seuils de
 * {@code KpiDefinition} restent ceux du catalogue, qui disent la santé de
 * l'indicateur ; ceux portés ici sont l'engagement pris devant la direction.
 *
 * <p><b>L'unité et le sens d'amélioration ne sont PAS recopiés</b> : ils
 * appartiennent au KPI. Un objectif qui porterait son unité pourrait la
 * contredire, et c'est la cible affichée qui mentirait. C'est aussi ce que la
 * maquette demande — pas d'unité en dur sur les champs de cible et de seuils,
 * puisqu'elle change d'un indicateur à l'autre.
 *
 * <p>Voir l'ADR 0133.
 */
@Entity
@Table(name = "quality_objectives",
        indexes = {
                @Index(name = "ix_quality_objectives_tenant_status",
                        columnList = "tenant_id, status"),
                @Index(name = "ix_quality_objectives_kpi", columnList = "tenant_id, kpi_id")
        })
public class QualityObjective {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 250)
    private String title;

    /** Le KPI réel auquel l'objectif se compare ; sans lui, §6.2 n'est pas tenu. */
    @Column(name = "kpi_id", nullable = false)
    private UUID kpiId;

    /**
     * Le processus concerné, repris du référentiel documentaire.
     *
     * <p>L'identifiant fait le lien, le libellé est FIGÉ à la saisie : un
     * processus renommé ne doit pas réécrire l'objectif d'une revue déjà tenue
     * (ADR 0052).
     */
    @Column(name = "process_id")
    private UUID processId;

    @Column(name = "process_name", length = 120)
    private String processName;

    @Column(name = "iso_clause", length = 40)
    private String isoClause;

    /** 1, 3, 6 ou 12 — voir {@link ObjectivePeriod}. */
    @Column(name = "period_months", nullable = false)
    private int periodMonths;

    @Column(name = "target_value", nullable = false, precision = 24, scale = 6)
    private BigDecimal targetValue;

    @Column(name = "threshold_warning", nullable = false, precision = 24, scale = 6)
    private BigDecimal thresholdWarning;

    @Column(name = "threshold_critical", nullable = false, precision = 24, scale = 6)
    private BigDecimal thresholdCritical;

    /** Périodes consécutives au-delà du seuil d'alerte avant de proposer une CAPA. */
    @Column(name = "warning_periods", nullable = false)
    private int warningPeriods;

    /** Périodes consécutives au-delà du seuil critique avant de proposer une CAPA. */
    @Column(name = "critical_periods", nullable = false)
    private int criticalPeriods;

    /**
     * Périodes consécutives SOUS LA CIBLE exigées pour redevenir « atteint ».
     *
     * <p>C'est aussi, mot pour mot, le critère d'efficacité de la CAPA née de cet
     * objectif : le retour du KPI sous la cible sur une durée définie. Un seul
     * chiffre pour les deux, parce que ce sont la même question posée deux fois.
     */
    @Column(name = "recovery_periods", nullable = false)
    private int recoveryPeriods;

    /**
     * Le pilote à qui la CAPA sera proposée ; choisi dans l'annuaire, son nom
     * recopié à la saisie (ADR 0052) — une fiche de revue n'affiche pas un UUID.
     */
    @Column(name = "capa_owner_id", nullable = false)
    private UUID capaOwnerId;

    @Column(name = "capa_owner_name", length = 255)
    private String capaOwnerName;

    /** Délai d'analyse de cause racine, en jours ouvrés ; échéance de la CAPA proposée. */
    @Column(name = "root_cause_due_days", nullable = false)
    private int rootCauseDueDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ObjectiveLifecycle status;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = ObjectiveLifecycle.ACTIVE;
        if (periodMonths == 0) periodMonths = ObjectivePeriod.MONTHLY.months();
        if (warningPeriods == 0) warningPeriods = 2;
        if (criticalPeriods == 0) criticalPeriods = 1;
        if (recoveryPeriods == 0) recoveryPeriods = 1;
        if (rootCauseDueDays == 0) rootCauseDueDays = 5;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    /** La période de cet objectif, nommée. */
    public ObjectivePeriod period() { return ObjectivePeriod.ofMonths(periodMonths); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public UUID getKpiId() { return kpiId; }
    public void setKpiId(UUID kpiId) { this.kpiId = kpiId; }
    public UUID getProcessId() { return processId; }
    public void setProcessId(UUID processId) { this.processId = processId; }
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }
    public String getIsoClause() { return isoClause; }
    public void setIsoClause(String isoClause) { this.isoClause = isoClause; }
    public int getPeriodMonths() { return periodMonths; }
    public void setPeriodMonths(int periodMonths) { this.periodMonths = periodMonths; }
    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }
    public BigDecimal getThresholdWarning() { return thresholdWarning; }
    public void setThresholdWarning(BigDecimal v) { this.thresholdWarning = v; }
    public BigDecimal getThresholdCritical() { return thresholdCritical; }
    public void setThresholdCritical(BigDecimal v) { this.thresholdCritical = v; }
    public int getWarningPeriods() { return warningPeriods; }
    public void setWarningPeriods(int warningPeriods) { this.warningPeriods = warningPeriods; }
    public int getCriticalPeriods() { return criticalPeriods; }
    public void setCriticalPeriods(int criticalPeriods) { this.criticalPeriods = criticalPeriods; }
    public int getRecoveryPeriods() { return recoveryPeriods; }
    public void setRecoveryPeriods(int recoveryPeriods) { this.recoveryPeriods = recoveryPeriods; }
    public UUID getCapaOwnerId() { return capaOwnerId; }
    public void setCapaOwnerId(UUID capaOwnerId) { this.capaOwnerId = capaOwnerId; }
    public String getCapaOwnerName() { return capaOwnerName; }
    public void setCapaOwnerName(String capaOwnerName) { this.capaOwnerName = capaOwnerName; }
    public int getRootCauseDueDays() { return rootCauseDueDays; }
    public void setRootCauseDueDays(int d) { this.rootCauseDueDays = d; }
    public ObjectiveLifecycle getStatus() { return status; }
    public void setStatus(ObjectiveLifecycle status) { this.status = status; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
